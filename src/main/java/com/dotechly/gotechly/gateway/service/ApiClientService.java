package com.dotechly.gotechly.gateway.service;

import com.dotechly.gotechly.gateway.domain.ApiClient;
import com.dotechly.gotechly.gateway.repository.ApiClientRepository;
import com.dotechly.gotechly.gateway.service.dto.ApiClientCredentialsDTO;
import com.dotechly.gotechly.gateway.service.dto.ApiClientDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Issues, stores and verifies machine-to-machine credentials.
 *
 * <p><strong>Blocking work is kept off the event loop.</strong> BCrypt is deliberately slow and this
 * is a WebFlux application, so every hash and every verification runs on
 * {@link Schedulers#boundedElastic()}. Calling {@code PasswordEncoder} inline on a Netty thread
 * would stall unrelated requests under any real token load.
 */
@Service
public class ApiClientService {

    private static final Logger LOG = LoggerFactory.getLogger(ApiClientService.class);

    /** Prefix makes a leaked key recognisable in logs and greppable in a secret scanner. */
    private static final String CLIENT_ID_PREFIX = "gtc_";

    private static final int CLIENT_ID_RANDOM_BYTES = 12;

    /** 256 bits of entropy. Stays under BCrypt's 72-byte input limit once Base64-encoded. */
    private static final int CLIENT_SECRET_BYTES = 32;

    /**
     * A valid BCrypt hash of a value nothing will ever match, used to burn the same CPU on an
     * unknown client id as on a wrong secret. Without it, "no such client" returns measurably faster
     * than "bad secret" and the endpoint becomes a client-id oracle.
     */
    private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final ApiClientRepository apiClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApiClientStatusCache statusCache;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder base64 = Base64.getUrlEncoder().withoutPadding();

    public ApiClientService(
        ApiClientRepository apiClientRepository,
        PasswordEncoder passwordEncoder,
        ApiClientStatusCache statusCache
    ) {
        this.apiClientRepository = apiClientRepository;
        this.passwordEncoder = passwordEncoder;
        this.statusCache = statusCache;
    }

    // ------------------------------------------------------------------------ administration

    /**
     * Create a client and mint its first secret.
     *
     * @return the stored client plus the plaintext secret — the only time it exists outside the
     *         caller's own storage
     */
    public Mono<ApiClientCredentialsDTO> create(ApiClientDTO request, String createdBy) {
        String secret = randomSecret();
        ApiClient client = new ApiClient();
        client.setClientId(randomClientId());
        client.setName(request.getName().trim());
        client.setDescription(trimToNull(request.getDescription()));
        client.setExpiresAt(request.getExpiresAt());
        client.setEnabled(true);
        client.setCreatedBy(createdBy);
        client.setCreatedDate(LocalDateTime.now());

        return hash(secret)
            .map(hashed -> {
                client.setClientSecretHash(hashed);
                return client;
            })
            .flatMap(apiClientRepository::save)
            .map(saved -> {
                LOG.info("API client created: clientId={} name='{}' by={}",
                    saved.getClientId(), saved.getName(), createdBy);
                return new ApiClientCredentialsDTO(new ApiClientDTO(saved), secret);
            });
    }

    /** Replace the secret, keeping the same client id so callers only swap one value. */
    public Mono<ApiClientCredentialsDTO> rotateSecret(Long id, String rotatedBy) {
        String secret = randomSecret();
        return apiClientRepository.findById(id)
            .flatMap(client -> hash(secret).map(hashed -> {
                client.setClientSecretHash(hashed);
                return client;
            }))
            .flatMap(apiClientRepository::save)
            .map(saved -> {
                LOG.info("API client secret rotated: clientId={} by={}", saved.getClientId(), rotatedBy);
                return new ApiClientCredentialsDTO(new ApiClientDTO(saved), secret);
            });
    }

    /**
     * Revoke or restore a client. Revoking is preferred over deleting: the row keeps the audit
     * trail (who created it, when it was last used) and the client id can never be reissued.
     */
    public Mono<ApiClientDTO> setEnabled(Long id, boolean enabled, String changedBy) {
        return apiClientRepository.findById(id)
            .map(client -> {
                client.setEnabled(enabled);
                return client;
            })
            .flatMap(apiClientRepository::save)
            .map(saved -> {
                // Kill-switch: drop the cached status so the very next request on this instance
                // re-reads the row. Other instances converge within their cache TTL.
                statusCache.invalidate(saved.getClientId());
                LOG.info("API client {}: clientId={} by={}",
                    enabled ? "enabled" : "revoked", saved.getClientId(), changedBy);
                return new ApiClientDTO(saved);
            });
    }

    /** Update the descriptive fields. Never touches the secret or the client id. */
    public Mono<ApiClientDTO> update(Long id, ApiClientDTO request) {
        return apiClientRepository.findById(id)
            .map(client -> {
                client.setName(request.getName().trim());
                client.setDescription(trimToNull(request.getDescription()));
                client.setExpiresAt(request.getExpiresAt());
                return client;
            })
            .flatMap(apiClientRepository::save)
            .map(saved -> {
                // expiresAt is part of the status check, so a change here must not be masked by a
                // stale cache entry.
                statusCache.invalidate(saved.getClientId());
                return new ApiClientDTO(saved);
            });
    }

    public Flux<ApiClientDTO> findAll() {
        return apiClientRepository.findAllByOrderByCreatedDateDesc().map(ApiClientDTO::new);
    }

    public Mono<ApiClientDTO> findOne(Long id) {
        return apiClientRepository.findById(id).map(ApiClientDTO::new);
    }

    // ------------------------------------------------------------------------ authentication

    /**
     * Verify a client id / secret pair.
     *
     * @return the client when the secret matches and the credential is enabled and unexpired;
     *         an empty {@link Mono} for every failure, so the caller cannot leak which check failed
     */
    public Mono<ApiClient> verifyCredentials(String clientId, String clientSecret) {
        if (isBlank(clientId) || isBlank(clientSecret)) {
            return Mono.empty();
        }
        return apiClientRepository.findOneByClientId(clientId.trim())
            .flatMap(client -> matches(clientSecret, client.getClientSecretHash())
                .flatMap(matched -> {
                    if (!matched) {
                        LOG.warn("API token request rejected: bad secret for clientId={}", clientId);
                        return Mono.empty();
                    }
                    if (!client.isUsableAt(LocalDateTime.now())) {
                        LOG.warn("API token request rejected: clientId={} is {}", clientId,
                            client.isEnabled() ? "expired" : "revoked");
                        return Mono.empty();
                    }
                    return Mono.just(client);
                }))
            .switchIfEmpty(Mono.defer(() -> unknownClient(clientId, clientSecret)));
    }

    /**
     * Record the successful use. Fire-and-forget: a failure to write the timestamp must never fail
     * the token request that already succeeded.
     */
    public Mono<Void> touchLastUsed(ApiClient client) {
        client.setLastUsedDate(LocalDateTime.now());
        return apiClientRepository.save(client)
            .doOnError(e -> LOG.warn("Could not record last-used for clientId={}: {}",
                client.getClientId(), e.getMessage()))
            .onErrorComplete()
            .then();
    }

    /** Burn comparable CPU on an unknown client id, then report nothing. */
    private Mono<ApiClient> unknownClient(String clientId, String clientSecret) {
        return matches(clientSecret, DUMMY_HASH)
            .doOnNext(ignored -> LOG.warn("API token request rejected: unknown clientId={}", clientId))
            .then(Mono.empty());
    }

    // ------------------------------------------------------------------------------ internals

    private Mono<String> hash(String raw) {
        return Mono.fromCallable(() -> passwordEncoder.encode(raw))
            .subscribeOn(Schedulers.boundedElastic());
    }

    private Mono<Boolean> matches(String raw, String hashed) {
        return Mono.fromCallable(() -> passwordEncoder.matches(raw, hashed))
            .subscribeOn(Schedulers.boundedElastic());
    }

    private String randomClientId() {
        byte[] bytes = new byte[CLIENT_ID_RANDOM_BYTES];
        secureRandom.nextBytes(bytes);
        return CLIENT_ID_PREFIX + base64.encodeToString(bytes);
    }

    private String randomSecret() {
        byte[] bytes = new byte[CLIENT_SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return base64.encodeToString(bytes);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
