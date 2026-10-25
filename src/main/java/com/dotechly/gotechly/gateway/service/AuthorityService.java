package com.dotechly.gotechly.gateway.service;

import com.dotechly.gotechly.gateway.domain.Authority;
import com.dotechly.gotechly.gateway.repository.AuthorityRepository;
import com.dotechly.gotechly.gateway.service.dto.AuthorityDTO;
import com.dotechly.gotechly.gateway.service.mapper.AuthorityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service Implementation for managing {@link Authority}.
 */
@Service
@Transactional("connectionFactoryTransactionManager")
public class AuthorityService {

    private final AuthorityRepository authorityRepository;
    private final AuthorityMapper authorityMapper;

    public AuthorityService(AuthorityRepository authorityRepository, AuthorityMapper authorityMapper) {
        this.authorityRepository = authorityRepository;
        this.authorityMapper = authorityMapper;
    }

    /**
     * Save a authority.
     *
     * @param authorityDTO the entity to save.
     * @return the persisted entity.
     */
    public Mono<AuthorityDTO> save(AuthorityDTO authorityDTO) {
        return authorityRepository.save(authorityMapper.toEntity(authorityDTO)).map(authorityMapper::toDto);
    }

    /**
     * Get all the authorities.
     *
     * @return the list of entities.
     */
    @Transactional(value = "connectionFactoryTransactionManager", readOnly = true)
    public Flux<AuthorityDTO> findAll() {
        return authorityRepository.findAll().map(authorityMapper::toDto);
    }

    /**
     * Get one authority by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(value = "connectionFactoryTransactionManager", readOnly = true)
    public Mono<AuthorityDTO> findOne(String id) {
        return authorityRepository.findById(id).map(authorityMapper::toDto);
    }

    /**
     * Delete the authority by id.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion is done.
     */
    public Mono<Void> delete(String id) {
        return authorityRepository.deleteById(id);
    }
}
