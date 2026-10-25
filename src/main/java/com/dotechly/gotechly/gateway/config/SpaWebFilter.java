package com.dotechly.gotechly.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Forwards non-API, non-static routes to index.html so Angular's router can handle them.
 * Must run before Spring Security's WebFilterChainProxy so the rewritten path
 * matches the permitAll rule for static resources.
 *
 * When a dedicated webapp port is configured, enforces content isolation:
 * - Webapp port: serves SPA/static content + API paths (full access)
 * - All other ports: serve only API paths, blocks SPA/static content
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpaWebFilter implements WebFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SpaWebFilter.class);

    private final int mainServerPort;
    private final int webappPort;
    private final int webappPartnerPort;

    public SpaWebFilter(
            @Value("${server.port:8080}") int mainServerPort,
            MultiPortProperties multiPortProperties) {
        this.mainServerPort = mainServerPort;
        this.webappPort = multiPortProperties.getWebappPort();
        this.webappPartnerPort = multiPortProperties.getWebappPartnerPort();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        int localPort = resolveLocalPort(exchange);

        // When webapp ports are configured, enforce content isolation
        if (webappPort > 0 || webappPartnerPort > 0) {
            if (localPort == webappPort || localPort == webappPartnerPort) {
                // Webapp/partner portal port: serve SPA + API, fall through to SPA forwarding
            } else {
                // API ports (main + additional): block SPA/static content
                if (!isApiPath(path)) {
                    LOG.debug("Static/SPA path {} blocked on API port {}", path, localPort);
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
                // API path on API port — let it through, no SPA forwarding needed
                return chain.filter(exchange);
            }
        }

        // SPA forwarding: rewrite deep-link routes to /index.html for Angular router
        if (!isApiPath(path) && !path.contains(".") && !path.equals("/")) {
            return chain.filter(
                exchange.mutate().request(exchange.getRequest().mutate().path("/index.html").build()).build()
            );
        }
        return chain.filter(exchange);
    }

    private boolean isApiPath(String path) {
        return path.startsWith("/api/") ||
               path.startsWith("/services/") ||
               path.startsWith("/management/") ||
               path.startsWith("/v3/api-docs");
    }

    private int resolveLocalPort(ServerWebExchange exchange) {
        InetSocketAddress localAddress = exchange.getRequest().getLocalAddress();
        return localAddress != null ? localAddress.getPort() : mainServerPort;
    }
}
