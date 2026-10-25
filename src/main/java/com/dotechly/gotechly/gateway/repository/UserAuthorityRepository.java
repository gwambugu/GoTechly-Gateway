package com.dotechly.gotechly.gateway.repository;

import com.dotechly.gotechly.gateway.domain.UserAuthority;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the {@link UserAuthority} join table.
 */
@Repository
public interface UserAuthorityRepository extends R2dbcRepository<UserAuthority, Long> {

    Flux<UserAuthority> findAllByUserId(Long userId);

    Mono<Void> deleteAllByUserId(Long userId);
}
