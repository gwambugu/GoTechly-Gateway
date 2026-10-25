package com.dotechly.gotechly.gateway.service;

import com.dotechly.gotechly.gateway.domain.Authority;
import com.dotechly.gotechly.gateway.domain.User;
import com.dotechly.gotechly.gateway.domain.UserAuthority;
import com.dotechly.gotechly.gateway.repository.AuthorityRepository;
import com.dotechly.gotechly.gateway.repository.UserAuthorityRepository;
import com.dotechly.gotechly.gateway.repository.UserRepository;
import com.dotechly.gotechly.gateway.service.dto.AdminUserDTO;
import com.dotechly.gotechly.gateway.service.mapper.UserMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.dotechly.gotechly.gateway.security.AuthoritiesConstants;
import com.dotechly.gotechly.gateway.security.SecurityUtils;
import com.dotechly.gotechly.gateway.service.util.RandomUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Service Implementation for managing {@link User}.
 */
@Service
@Transactional("connectionFactoryTransactionManager")
public class UserService {

    private final UserRepository userRepository;
    private final UserAuthorityRepository userAuthorityRepository;
    private final AuthorityRepository authorityRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    /** Present only when AD is enabled; absent in local/dev mode. */
    private final ObjectProvider<AdDirectoryService> adDirectoryProvider;

    public UserService(UserRepository userRepository,
                       UserAuthorityRepository userAuthorityRepository,
                       AuthorityRepository authorityRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder,
                       MailService mailService,
                       ObjectProvider<AdDirectoryService> adDirectoryProvider) {
        this.userRepository = userRepository;
        this.userAuthorityRepository = userAuthorityRepository;
        this.authorityRepository = authorityRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.adDirectoryProvider = adDirectoryProvider;
    }

    public Mono<User> activateRegistration(String key) {
        return userRepository.findOneByActivationKey(key)
                .flatMap(user -> {
                    user.setActivated(true);
                    user.setActivationKey(null);
                    return userRepository.save(user);
                });
    }

    /**
     * Single canonicalisation point for a login. With AD enabled this is the UPN
     * ({@code jdoe} → {@code jdoe@corp.example.com}); without AD it is just lowercased.
     * Registration, the availability check and {@code AdReactiveAuthenticationManager} must all
     * use it, or a user who registers as {@code jdoe@corp.example.com} cannot sign in as {@code jdoe}.
     */
    public String normalizeLogin(String login) {
        AdDirectoryService ad = adDirectoryProvider.getIfAvailable();
        return ad == null ? (login == null ? null : login.trim().toLowerCase()) : ad.toUpn(login);
    }

    public Mono<User> registerUser(AdminUserDTO userDTO, String password) {
        String entered = normalizeLogin(userDTO.getLogin());
        AdDirectoryService ad = adDirectoryProvider.getIfAvailable();

        // When AD is enabled, the account must exist in the directory before we create a pending
        // local record. No password is verified here — that happens at login. The directory's own
        // userPrincipalName becomes the stored login, not what was typed: the search filter also
        // matches on mail, and mail is often in a different domain than the UPN, which would
        // produce a local row that can never bind.
        Mono<String> resolvedLogin = (ad == null)
                ? Mono.just(entered)
                : ad.findUpn(entered).flatMap(found -> found
                .<Mono<String>>map(Mono::just)
                .orElseGet(() -> Mono.error(new RuntimeException("User not found in directory"))));

        return resolvedLogin.flatMap(login -> createPendingUser(userDTO, password, login, ad));
    }

    private Mono<User> createPendingUser(AdminUserDTO userDTO, String password, String login, AdDirectoryService ad) {
        return userRepository.findOneByLogin(login)
                .flatMap(existingUser -> {
                    if (!existingUser.isActivated()) {
                        return userRepository.delete(existingUser);
                    }
                    return Mono.error(new RuntimeException("Login already in use"));
                })
                .then(userRepository.findOneByEmailIgnoreCase(userDTO.getEmail()))
                .flatMap(existingUser -> {
                    if (!existingUser.isActivated()) {
                        return userRepository.delete(existingUser);
                    }
                    return Mono.error(new RuntimeException("Email already in use"));
                })
                .then(Mono.defer(() -> {
                    User newUser = new User();
                    newUser.setLogin(login);
                    if (ad == null) {
                        // Local/dev fallback: password comes from the form, BCrypt-checked at login.
                        newUser.setAuthSource("LOCAL");
                        newUser.setPassword(passwordEncoder.encode(password));
                    } else {
                        // AD-managed: store an unusable random hash (keeps NOT NULL / length valid);
                        // the AD bind is the real gate, this hash can never authenticate.
                        newUser.setAuthSource("AD");
                        newUser.setPassword(passwordEncoder.encode(RandomUtil.generatePassword()));
                    }
                    newUser.setFirstName(userDTO.getFirstName());
                    newUser.setLastName(userDTO.getLastName());
                    if (userDTO.getEmail() != null) {
                        newUser.setEmail(userDTO.getEmail().toLowerCase());
                    }
                    newUser.setImageUrl(userDTO.getImageUrl());
                    newUser.setLangKey(userDTO.getLangKey());
                    // Pending until an admin approves and assigns a role — admin approval is the
                    // only path to activated=true (no self-activation email/key).
                    newUser.setActivated(false);
                    return userRepository.save(newUser);
                }))
                .flatMap(savedUser -> userAuthorityRepository.save(new UserAuthority(savedUser.getId(), AuthoritiesConstants.USER)).thenReturn(savedUser));
    }

    public Mono<Void> changePassword(String currentClearTextPassword, String newPassword) {
        return SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneByLogin)
                .flatMap(user -> {
                    String currentEncryptedPassword = user.getPassword();
                    if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
                        return Mono.error(new RuntimeException("Invalid password"));
                    }
                    String encryptedPassword = passwordEncoder.encode(newPassword);
                    user.setPassword(encryptedPassword);
                    return userRepository.save(user);
                })
                .then();
    }

    public Mono<User> requestPasswordReset(String mail) {
        return userRepository.findOneByEmailIgnoreCase(mail)
                .filter(User::isActivated)
                .flatMap(user -> {
                    user.setResetKey(RandomUtil.generateResetKey());
                    user.setResetDate(Instant.now());
                    return userRepository.save(user);
                })
                .doOnNext(mailService::sendPasswordResetEmail);
    }

    public Mono<User> completePasswordReset(String newPassword, String key) {
        return userRepository.findOneByResetKey(key)
                .filter(user -> user.getResetDate().isAfter(Instant.now().minus(1, ChronoUnit.DAYS)))
                .flatMap(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setResetKey(null);
                    user.setResetDate(null);
                    return userRepository.save(user);
                });
    }

    /**
     * Create a user.
     *
     * @param adminUserDTO the entity to create.
     * @return the persisted entity.
     */
    public Mono<User> createUser(AdminUserDTO adminUserDTO) {
        User user = userMapper.adminUserDTOToUser(adminUserDTO);
        // Set default values if needed
        return userRepository.save(user)
                .flatMap(savedUser -> {
                    if (adminUserDTO.getAuthorities() != null) {
                        return Flux.fromIterable(adminUserDTO.getAuthorities())
                                .flatMap(authorityName -> userAuthorityRepository.save(new UserAuthority(savedUser.getId(), authorityName)))
                                .then(Mono.just(savedUser));
                    }
                    return Mono.just(savedUser);
                });
    }

    /**
     * Update a user.
     *
     * @param adminUserDTO the entity to update.
     * @return the persisted entity.
     */
    public Mono<AdminUserDTO> updateUser(AdminUserDTO adminUserDTO) {
        return userRepository.findById(adminUserDTO.getId())
                .flatMap(user -> {
                    user.setLogin(adminUserDTO.getLogin());
                    user.setFirstName(adminUserDTO.getFirstName());
                    user.setLastName(adminUserDTO.getLastName());
                    user.setEmail(adminUserDTO.getEmail());
                    user.setActivated(adminUserDTO.isActivated());
                    user.setLangKey(adminUserDTO.getLangKey());
                    user.setImageUrl(adminUserDTO.getImageUrl());
                    return userRepository.save(user);
                })
                .flatMap(user -> userAuthorityRepository.deleteAllByUserId(user.getId()).thenReturn(user))
                .flatMap(user -> {
                    if (adminUserDTO.getAuthorities() != null) {
                        return Flux.fromIterable(adminUserDTO.getAuthorities())
                                .flatMap(authorityName -> userAuthorityRepository.save(new UserAuthority(user.getId(), authorityName)))
                                .then(Mono.just(user));
                    }
                    return Mono.just(user);
                })
                .flatMap(this::fetchAuthorities)
                .map(userMapper::userToAdminUserDTO);
    }

    /**
     * Get all users.
     *
     * @param pageable the pagination information.
     * @return the list of users.
     */
    @Transactional(value = "connectionFactoryTransactionManager", readOnly = true)
    public Flux<AdminUserDTO> getAllManagedUsers(Pageable pageable) {
        return userRepository.findAllByIdNotNull(pageable).flatMap(this::fetchAuthorities).map(userMapper::userToAdminUserDTO);
    }

    @Transactional(value = "connectionFactoryTransactionManager", readOnly = true)
    public Mono<Long> countUsers() {
        return userRepository.count();
    }

    /**
     * Get one user by login.
     *
     * @param login the login of the user.
     * @return the user.
     */
    @Transactional(value = "connectionFactoryTransactionManager", readOnly = true)
    public Mono<AdminUserDTO> getUserWithAuthoritiesByLogin(String login) {
        return userRepository.findOneByLogin(login).flatMap(this::fetchAuthorities).map(userMapper::userToAdminUserDTO);
    }

    /**
     * Get one user by id.
     *
     * @param id the id of the user.
     * @return the user.
     */
    @Transactional(value = "connectionFactoryTransactionManager", readOnly = true)
    public Mono<AdminUserDTO> getUserWithAuthorities(Long id) {
        return userRepository.findById(id).flatMap(this::fetchAuthorities).map(userMapper::userToAdminUserDTO);
    }

    /**
     * Delete the user by login.
     *
     * @param login the login of the user.
     * @return a Mono to signal the deletion is done.
     */
    public Mono<Void> deleteUser(String login) {
        return userRepository.findOneByLogin(login)
                .flatMap(user -> userAuthorityRepository.deleteAllByUserId(user.getId()).thenReturn(user))
                .flatMap(userRepository::delete);
    }

    /**
     * Delete the user by id.
     *
     * @param id the id of the user.
     * @return a Mono to signal the deletion is done.
     */
    public Mono<Void> deleteUserById(Long id) {
        return userRepository.findById(id)
                .flatMap(user -> userAuthorityRepository.deleteAllByUserId(user.getId()).thenReturn(user))
                .flatMap(userRepository::delete);
    }

    private Mono<User> fetchAuthorities(User user) {
        return userAuthorityRepository.findAllByUserId(user.getId())
                .map(UserAuthority::getAuthorityName)
                .map(name -> {
                    Authority auth = new Authority();
                    auth.setName(name);
                    return auth;
                })
                .collect(Collectors.toSet())
                .map(authorities -> {
                    user.setAuthorities(authorities);
                    return user;
                });
    }

    @Transactional(value = "connectionFactoryTransactionManager", readOnly = true)
    public Mono<Boolean> isLoginAvailable(String login) {
        return userRepository.findOneByLogin(normalizeLogin(login))
                .map(user -> !user.isActivated() ? Mono.just(true) : Mono.just(false))
                .defaultIfEmpty(Mono.just(true))
                .flatMap(mono -> mono);
    }

    @Transactional(value = "connectionFactoryTransactionManager", readOnly = true)
    public Mono<Boolean> isEmailAvailable(String email) {
        return userRepository.findOneByEmailIgnoreCase(email.toLowerCase())
                .map(user -> !user.isActivated() ? Mono.just(true) : Mono.just(false))
                .defaultIfEmpty(Mono.just(true))
                .flatMap(mono -> mono);
    }

    @Transactional(value = "connectionFactoryTransactionManager", readOnly = true)
    public Flux<AdminUserDTO> getPendingUsers() {
        return userRepository.findAllByActivatedIsFalse()
                .flatMap(this::fetchAuthorities)
                .map(userMapper::userToAdminUserDTO);
    }

    public Mono<Void> approveUser(Long id) {
        return userRepository.findById(id)
                .flatMap(user -> {
                    user.setActivated(true);
                    user.setActivationKey(null);
                    return userRepository.save(user);
                })
                .then();
    }

    @Transactional(value = "connectionFactoryTransactionManager", readOnly = true)
    public Flux<AdminUserDTO> searchUsers(String query) {
        return userRepository.findAllByLoginContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query)
                .flatMap(this::fetchAuthorities)
                .map(userMapper::userToAdminUserDTO);
    }

    public Mono<Void> updateUserRoles(Long id, Set<String> authorities) {
        return userRepository.findById(id)
                .flatMap(user -> userAuthorityRepository.deleteAllByUserId(user.getId())
                        .thenMany(Flux.fromIterable(authorities))
                        .flatMap(authName -> {
                            UserAuthority ua = new UserAuthority();
                            ua.setUserId(user.getId());
                            ua.setAuthorityName(authName);
                            return userAuthorityRepository.save(ua);
                        })
                        .then()
                )
                .cast(Void.class);
    }

    public Mono<Void> setUserEnabled(Long id, boolean enabled) {
        return userRepository.findById(id)
                .flatMap(user -> {
                    user.setActivated(enabled);
                    return userRepository.save(user);
                })
                .then();
    }

    @Transactional(value = "connectionFactoryTransactionManager", readOnly = true)
    public Flux<AdminUserDTO> getOnlineUsers() {
        return userRepository.findAllByActivatedIsTrue()
                .flatMap(this::fetchAuthorities)
                .map(userMapper::userToAdminUserDTO);
    }
}
