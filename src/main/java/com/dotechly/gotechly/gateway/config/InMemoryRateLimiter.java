package com.dotechly.gotechly.gateway.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryRateLimiter implements GlobalFilter, Ordered {

    private static final int MAX_REQUESTS_PER_SECOND = 100;
    private static final long WINDOW_MS = 1000;
    private static final long EVICTION_INTERVAL_SECONDS = 60;
    private static final long BUCKET_IDLE_THRESHOLD_MS = 60_000;

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private ScheduledExecutorService evictionScheduler;

    @PostConstruct
    void startEviction() {
        evictionScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limiter-eviction");
            t.setDaemon(true);
            return t;
        });
        evictionScheduler.scheduleAtFixedRate(this::evictIdleBuckets,
                EVICTION_INTERVAL_SECONDS, EVICTION_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stopEviction() {
        if (evictionScheduler != null) {
            evictionScheduler.shutdownNow();
        }
    }

    private void evictIdleBuckets() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(e -> now - e.getValue().lastRefill > BUCKET_IDLE_THRESHOLD_MS);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        // Only rate-limit proxied API requests
        if (!path.startsWith("/services/")) {
            return chain.filter(exchange);
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        String clientIp = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
        String key = clientIp;

        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(MAX_REQUESTS_PER_SECOND, WINDOW_MS));

        if (bucket.tryConsume()) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().set("Retry-After", "1");
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }

    private static class TokenBucket {
        private final int maxTokens;
        private final long windowMs;
        private final AtomicLong tokens;
        private volatile long lastRefill;

        TokenBucket(int maxTokens, long windowMs) {
            this.maxTokens = maxTokens;
            this.windowMs = windowMs;
            this.tokens = new AtomicLong(maxTokens);
            this.lastRefill = System.currentTimeMillis();
        }

        boolean tryConsume() {
            refill();
            return tokens.getAndUpdate(t -> t > 0 ? t - 1 : 0) > 0;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            if (now - lastRefill >= windowMs) {
                tokens.set(maxTokens);
                lastRefill = now;
            }
        }
    }
}
