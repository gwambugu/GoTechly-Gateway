package com.dotechly.gotechly.gateway.web.rest;

import com.dotechly.gotechly.gateway.service.AuthorityService;
import com.dotechly.gotechly.gateway.service.dto.AuthorityDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * REST controller for managing authorities.
 */
@RestController
@RequestMapping("/api/admin/authorities")
public class AuthorityResource {

    private final AuthorityService authorityService;

    public AuthorityResource(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    /**
     * {@code POST  /authorities} : Create a new authority.
     *
     * @param authorityDTO the authorityDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new authorityDTO.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping
    public Mono<ResponseEntity<AuthorityDTO>> createAuthority(@Valid @RequestBody AuthorityDTO authorityDTO) throws URISyntaxException {
        return authorityService.save(authorityDTO)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/admin/authorities/" + result.getName())).body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code GET  /authorities} : get all the authorities.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of authorities in body.
     */
    @GetMapping
    public Flux<AuthorityDTO> getAllAuthorities() {
        return authorityService.findAll();
    }

    /**
     * {@code GET  /authorities/:id} : get the "id" authority.
     *
     * @param id the id of the authorityDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the authorityDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<AuthorityDTO>> getAuthority(@PathVariable String id) {
        return authorityService.findOne(id)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * {@code DELETE  /authorities/:id} : delete the "id" authority.
     *
     * @param id the id of the authorityDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteAuthority(@PathVariable String id) {
        return authorityService.delete(id).thenReturn(ResponseEntity.noContent().build());
    }
}
