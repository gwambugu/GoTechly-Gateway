package com.dotechly.gotechly.gateway.web.rest;


import com.dotechly.gotechly.gateway.config.MultiPortProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * Tells the SPA which logical portal role it is running under, derived from the
 * local port that received the request. Lets the SPA stop hardcoding port
 * numbers — the YAML in {@code gotechly.gateway.multi-port} stays the single
 * source of truth.
 */
@RestController
@RequestMapping("/api/portal-context")
public class PortalContextResource {

    private final MultiPortProperties properties;

    public PortalContextResource(MultiPortProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public Mono<Map<String, Object>> get(ServerWebExchange exchange) {
        InetSocketAddress local = exchange.getRequest().getLocalAddress();
        int port = local != null ? local.getPort() : -1;

        String role;
        if (port == properties.getWebappPort()) {
            role = "ADMIN";
        } else if (port == properties.getWebappPartnerPort()) {
            role = "PARTNER";
        } else {
            role = "OTHER";
        }

        return Mono.just(Map.of(
                "role", role,
                "port", port
        ));
    }
}
