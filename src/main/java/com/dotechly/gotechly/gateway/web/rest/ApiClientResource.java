package com.dotechly.gotechly.gateway.web.rest;

import com.dotechly.gotechly.gateway.security.SecurityUtils;
import com.dotechly.gotechly.gateway.service.ApiClientService;
import com.dotechly.gotechly.gateway.service.dto.ApiClientCredentialsDTO;
import com.dotechly.gotechly.gateway.service.dto.ApiClientDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Admin API for machine-to-machine credentials.
 *
 * <p>Reachable only by {@code ROLE_ADMIN} — the {@code /api/admin/**} rule in
 * {@code SecurityConfiguration} covers this whole controller, so no per-method annotations are
 * needed.
 *
 * <p><strong>The plaintext secret appears in exactly two responses</strong> — create and
 * rotate — and is never stored, so it cannot be recovered afterwards. Everything else returns
 * {@link ApiClientDTO}, which has no secret field at all.
 */
@RestController
@RequestMapping("/api/admin/api-clients")
public class ApiClientResource {

    private final ApiClientService apiClientService;

    public ApiClientResource(ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    /** {@code POST /api/admin/api-clients} : create a client and return its one-time secret. */
    @PostMapping
    public Mono<ResponseEntity<ApiClientCredentialsDTO>> create(@Valid @RequestBody ApiClientDTO request) {
        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty("system")
            .flatMap(login -> apiClientService.create(request, login))
            .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created));
    }

    /** {@code GET /api/admin/api-clients} : list every client, newest first. */
    @GetMapping
    public Flux<ApiClientDTO> getAll() {
        return apiClientService.findAll();
    }

    /** {@code GET /api/admin/api-clients/:id} : one client. */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiClientDTO>> get(@PathVariable Long id) {
        return apiClientService.findOne(id)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /** {@code PUT /api/admin/api-clients/:id} : rename / re-describe / change expiry. */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiClientDTO>> update(@PathVariable Long id, @Valid @RequestBody ApiClientDTO request) {
        return apiClientService.update(id, request)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * {@code POST /api/admin/api-clients/:id/rotate-secret} : issue a new secret, same client id.
     * The old secret stops working immediately, so schedule the swap with the consumer.
     */
    @PostMapping("/{id}/rotate-secret")
    public Mono<ResponseEntity<ApiClientCredentialsDTO>> rotateSecret(@PathVariable Long id) {
        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty("system")
            .flatMap(login -> apiClientService.rotateSecret(id, login))
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * {@code POST /api/admin/api-clients/:id/revoke} : stop the credential working.
     *
     * <p>Preferred over deletion — the row keeps who created it and when it was last used, and the
     * client id can never be handed out again. Tokens already minted stay valid until they expire;
     * shorten {@code token-validity-in-seconds} if that window matters.
     */
    @PostMapping("/{id}/revoke")
    public Mono<ResponseEntity<ApiClientDTO>> revoke(@PathVariable Long id) {
        return setEnabled(id, false);
    }

    /** {@code POST /api/admin/api-clients/:id/enable} : restore a revoked credential. */
    @PostMapping("/{id}/enable")
    public Mono<ResponseEntity<ApiClientDTO>> enable(@PathVariable Long id) {
        return setEnabled(id, true);
    }

    private Mono<ResponseEntity<ApiClientDTO>> setEnabled(Long id, boolean enabled) {
        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty("system")
            .flatMap(login -> apiClientService.setEnabled(id, enabled, login))
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
