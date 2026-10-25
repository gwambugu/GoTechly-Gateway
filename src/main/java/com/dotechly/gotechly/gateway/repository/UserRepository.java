package com.dotechly.gotechly.gateway.repository;

import com.dotechly.gotechly.gateway.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Spring Data R2DBC repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, Long> {

    Mono<User> findOneByActivationKey(String activationKey);

    Mono<User> findOneByResetKey(String resetKey);

    Mono<User> findOneByEmailIgnoreCase(String email);

    Mono<User> findOneByLogin(String login);

    Flux<User> findAllByIdNotNull(Pageable pageable);

    Flux<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);

    Mono<Long> count();

    Flux<User> findAllByActivatedIsFalse();

    Flux<User> findAllByActivatedIsTrue();

    Flux<User> findAllByLoginContainingIgnoreCaseOrEmailContainingIgnoreCase(String login, String email);
}
