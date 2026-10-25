package com.dotechly.gotechly.gateway.security;

import com.dotechly.gotechly.gateway.domain.User;
import com.dotechly.gotechly.gateway.repository.UserAuthorityRepository;
import com.dotechly.gotechly.gateway.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Authenticate a user from the database.
 */
@Component("userDetailsService")
public class DomainUserDetailsService implements ReactiveUserDetailsService {

    private final Logger log = LoggerFactory.getLogger(DomainUserDetailsService.class);

    private final UserRepository userRepository;
    private final UserAuthorityRepository userAuthorityRepository;

    public DomainUserDetailsService(UserRepository userRepository, UserAuthorityRepository userAuthorityRepository) {
        this.userRepository = userRepository;
        this.userAuthorityRepository = userAuthorityRepository;
    }

    @Override
    @Transactional("connectionFactoryTransactionManager")
    public Mono<UserDetails> findByUsername(final String login) {
        log.debug("Authenticating {}", login);

        return userRepository.findOneByLogin(login)
            .flatMap(user -> userAuthorityRepository.findAllByUserId(user.getId())
                .map(ua -> new SimpleGrantedAuthority(ua.getAuthorityName()))
                .collectList()
                .map(authorities -> createSpringSecurityUser(login, user, authorities))
            )
            .cast(UserDetails.class)
            .switchIfEmpty(Mono.error(new UsernameNotFoundException("User " + login + " was not found in the database")));
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(String lowercaseLogin, User user, List<SimpleGrantedAuthority> grantedAuthorities) {
        if (!user.isActivated()) {
            throw new UserNotActivatedException("User " + lowercaseLogin + " was not activated");
        }
        return new org.springframework.security.core.userdetails.User(user.getLogin(), user.getPassword(), grantedAuthorities);
    }
}
