package com.dotechly.gotechly.gateway.repository;

import com.dotechly.gotechly.gateway.domain.ApiClient;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the {@link ApiClient} entity.
 */
@Repository
public interface ApiClientRepository extends R2dbcRepository<ApiClient, Long> {

    Mono<ApiClient> findOneByClientId(String clientId);

    Flux<ApiClient> findAllByOrderByCreatedDateDesc();
}
