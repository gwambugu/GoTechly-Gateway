package com.dotechly.gotechly.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Injects the internal API key header on all requests routed to Core.
 * <p>
 * Security: first strips any spoofed {@code X-Internal-Api-Key} header from the
 * incoming client request, then injects the real key before routing downstream.
 * This prevents external clients from forging the header through the Gateway.
 */
@Component
public class InternalApiKeyGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(InternalApiKeyGlobalFilter.class);

    private final String apiKey;
    private final String headerName;

    public InternalApiKeyGlobalFilter(
            @Value("${gotechly.security.internal-api-key.secret}") String apiKey,
            @Value("${gotechly.security.internal-api-key.header-name:X-Internal-Api-Key}") String headerName) {
        this.apiKey = apiKey;
        this.headerName = headerName;
        LOG.info("InternalApiKeyGlobalFilter initialized — header='{}', keyLength={}", headerName, apiKey.length());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove(headerName);       // Strip any spoofed header from client
                    h.set(headerName, apiKey);   // Inject real key
                })
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        // Run just before routing — after port access and rate limiting, before downstream dispatch
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
