package com.dotechly.gotechly.gateway.service;

import com.dotechly.gotechly.gateway.repository.ApiClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The revocation kill-switch.
 *
 * <p>A JWT is self-contained: once minted it stays cryptographically valid until it expires, so
 * revoking a client would otherwise leave its outstanding tokens working for the rest of their
 * lifetime. Every request bearing an {@code ROLE_API_CLIENT} token therefore re-checks that the
 * client is still active, and {@code JWTFilter} refuses to install a principal when it is not.
 *
 * <p>A per-request database round trip on the hot path would be unacceptable, so answers are cached
 * for a short TTL ({@code gotechly.security.api-client.status-cache-seconds}, default 30s). The
 * consequence is worth stating plainly:
 *
 * <ul>
 *   <li>On the instance that performed the revoke, it takes effect <strong>immediately</strong> —
 *       {@link ApiClientService} invalidates the entry directly.</li>
 *   <li>On other instances in the cluster, within <strong>one TTL</strong>.</li>
 * </ul>
 *
 * <p><strong>Failure behaviour:</strong> if the lookup fails, a previously cached answer is reused
 * even when stale, and a client never seen before is treated as inactive. A database blip must not
 * hand out access to an unknown credential, and equally must not sever every established
 * integration.
 */
@Component
public class ApiClientStatusCache {

    private static final Logger LOG = LoggerFactory.getLogger(ApiClientStatusCache.class);

    private final ApiClientRepository apiClientRepository;
    private final long ttlNanos;

    /** Entries are kept after expiry: a stale answer is the fallback when the database is down. */
    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();

    public ApiClientStatusCache(
        ApiClientRepository apiClientRepository,
        @Value("${gotechly.security.api-client.status-cache-seconds:30}") long statusCacheSeconds
    ) {
        this.apiClientRepository = apiClientRepository;
        this.ttlNanos = Duration.ofSeconds(Math.max(1, statusCacheSeconds)).toNanos();
    }

    /**
     * @return {@code true} when the credential behind this token may still be used — present,
     *         enabled and unexpired
     */
    public Mono<Boolean> isActive(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return Mono.just(false);
        }
        Entry cached = cache.get(clientId);
        if (cached != null && cached.isFresh(System.nanoTime())) {
            return Mono.just(cached.active());
        }
        return apiClientRepository.findOneByClientId(clientId)
            .map(client -> client.isUsableAt(LocalDateTime.now()))
            .defaultIfEmpty(false)
            .doOnNext(active -> cache.put(clientId, new Entry(active, System.nanoTime() + ttlNanos)))
            .onErrorResume(e -> {
                boolean fallback = cached != null && cached.active();
                LOG.warn("API client status lookup failed for clientId={}; falling back to {} ({})",
                    clientId, fallback ? "last known ACTIVE" : "INACTIVE", e.toString());
                return Mono.just(fallback);
            });
    }

    /** Drop a cached answer so the next request re-reads the database. */
    public void invalidate(String clientId) {
        if (clientId != null) {
            cache.remove(clientId);
        }
    }

    public void clear() {
        cache.clear();
    }

    private record Entry(boolean active, long expiresAtNanos) {

        boolean isFresh(long nowNanos) {
            return expiresAtNanos - nowNanos > 0;
        }
    }
}
