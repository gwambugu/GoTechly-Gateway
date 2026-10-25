package com.dotechly.gotechly.gateway.repository;

import com.dotechly.gotechly.gateway.domain.Authority;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data R2DBC repository for the {@link Authority} entity.
 */
@Repository
public interface AuthorityRepository extends R2dbcRepository<Authority, String> {
}
