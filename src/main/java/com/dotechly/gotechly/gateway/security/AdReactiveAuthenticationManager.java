package com.dotechly.gotechly.gateway.security;

import com.dotechly.gotechly.gateway.domain.User;
import com.dotechly.gotechly.gateway.repository.UserAuthorityRepository;
import com.dotechly.gotechly.gateway.repository.UserRepository;
import com.dotechly.gotechly.gateway.service.AdDirectoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Optional;

/**
 * Authenticates against Active Directory while keeping role ownership in this app.
 *
 * <p>Order (matches the product requirement):
 * <ol>
 *   <li>Resolve the local user; reject if absent.</li>
 *   <li>App-side gate: reject if not {@code activated} (pending/disabled) or has no role —
 *       <em>before</em> any AD call.</li>
 *   <li>{@code LOCAL} break-glass admin → BCrypt compare, never touches AD.</li>
 *   <li>{@code AD} user → bind AS the user. On bind failure, classify via
 *       {@code userAccountControl}: disabled ⇒ flip local {@code activated=false}; otherwise
 *       just bad credentials.</li>
 * </ol>
 *
 * <p>The returned authorities always come from this app's DB — AD groups are ignored.
 * Active only when {@code gotechly.security.ad.enabled=true}; otherwise the local
 * {@code ReactiveAuthenticationManager} in {@code SecurityConfiguration} is used.
 */
@Component
@ConditionalOnProperty(prefix = "gotechly.security.ad", name = "enabled", havingValue = "true")
public class AdReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(AdReactiveAuthenticationManager.class);

    private final UserRepository userRepository;
    private final UserAuthorityRepository userAuthorityRepository;
    private final ActiveDirectoryLdapAuthenticationProvider adProvider;
    private final AdDirectoryService adDirectory;
    private final PasswordEncoder passwordEncoder;

    public AdReactiveAuthenticationManager(UserRepository userRepository,
                                           UserAuthorityRepository userAuthorityRepository,
                                           ActiveDirectoryLdapAuthenticationProvider adProvider,
                                           AdDirectoryService adDirectory,
                                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userAuthorityRepository = userAuthorityRepository;
        this.adProvider = adProvider;
        this.adDirectory = adDirectory;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        // Canonical UPN — a user who typed the bare directory name resolves to the same
        // local row (and the same AD bind principal) as one who typed the full UPN.
        String login = adDirectory.toUpn(authentication.getName());
        String password = (String) authentication.getCredentials();

        return userRepository.findOneByLogin(login)
                .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid credentials")))
                .flatMap(user -> {
                    // (1) App-side gate — runs before we ever talk to AD.
                    if (!user.isActivated()) {
                        return Mono.error(new DisabledException("Account is pending approval or disabled"));
                    }
                    return loadAuthorities(user.getId()).flatMap(authorities -> {
                        if (authorities.isEmpty()) {
                            return Mono.error(new DisabledException("No role assigned"));
                        }
                        // (2) LOCAL break-glass admin → BCrypt, never touches AD.
                        if ("LOCAL".equalsIgnoreCase(user.getAuthSource())) {
                            if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
                                return Mono.error(new BadCredentialsException("Invalid credentials"));
                            }
                            return Mono.just(success(login, authorities));
                        }
                        // (3) AD user → bind as the user.
                        return authenticateAgainstAd(login, password, authorities, user);
                    });
                });
    }

    private Mono<Authentication> authenticateAgainstAd(String login, String password,
                                                       List<SimpleGrantedAuthority> authorities, User user) {
        return Mono.fromCallable(() -> {
                    adProvider.authenticate(new UsernamePasswordAuthenticationToken(login, password));
                    return success(login, authorities);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(AuthenticationException.class, ex -> classifyFailure(login, user, ex));
    }

    /**
     * Bind failed — use userAccountControl as the source of truth, not the bind error.
     *
     * <p>The provider's own exception is logged rather than discarded: it carries the AD
     * sub-error code ({@code 52e} bad password, {@code 525} no such user, {@code 530}/{@code 531}
     * logon-hours/workstation, {@code 773} must-change-password), which is the only thing that
     * distinguishes a typo from a malformed bind principal. Typed states produced by
     * {@code setConvertSubErrorCodesToExceptions} are rethrown as-is — collapsing them all into
     * "Invalid credentials" made a locked or expired account look like a wrong password.
     */
    private Mono<Authentication> classifyFailure(String login, User user, AuthenticationException cause) {
        log.warn("AD bind failed for login={} (bindPrincipal={}): {}",
                login, adDirectory.bindPrincipal(login), cause.getMessage(), cause);
        return adDirectory.isDisabledInDirectory(login)
                // A directory error here is not an authentication outcome. Letting it escape turned a
                // failed login into a 500 (Spring LDAP throws DataAccessException, which no
                // onErrorResume upstream catches) and hid the bind failure that actually mattered.
                .onErrorResume(ex -> {
                    log.warn("userAccountControl lookup for {} failed; treating as not-disabled", login, ex);
                    return Mono.just(Optional.<Boolean>empty());
                })
                .flatMap(disabled -> {
                    if (disabled.orElse(false)) {
                        return disableLocally(user)
                                .then(Mono.error(new DisabledException("Account disabled in directory")));
                    }
                    if (cause instanceof LockedException
                            || cause instanceof CredentialsExpiredException
                            || cause instanceof AccountExpiredException
                            || cause instanceof DisabledException) {
                        return Mono.error(cause);
                    }
                    return Mono.error(new BadCredentialsException("Invalid credentials"));
                });
    }

    private Mono<Void> disableLocally(User user) {
        log.warn("AD reports {} disabled — flipping local activated=false", user.getLogin());
        user.setActivated(false);
        return userRepository.save(user).then();
    }

    private Mono<List<SimpleGrantedAuthority>> loadAuthorities(Long userId) {
        return userAuthorityRepository.findAllByUserId(userId)
                .map(ua -> new SimpleGrantedAuthority(ua.getAuthorityName()))
                .collectList();
    }

    private Authentication success(String login, List<SimpleGrantedAuthority> authorities) {
        return new UsernamePasswordAuthenticationToken(login, null, authorities);
    }
}