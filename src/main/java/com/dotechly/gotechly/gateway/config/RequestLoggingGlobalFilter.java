package com.dotechly.gotechly.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Unconditional access log for every request entering Gateway. Implemented
 * as a Spring WebFilter (not a Spring Cloud Gateway GlobalFilter) so it
 * runs at the WebFlux dispatcher level — BEFORE route matching. This
 * captures requests that no Gateway route matches (which would otherwise
 * 404 silently without any GlobalFilter ever running) as well as requests
 * blocked by security or other WebFlux filters.
 *
 * Logs: source IP, local listening port (so you can tell which named
 * port the request landed on), method, path, status, elapsed millis.
 *
 * Added to triage partner integration issues where a request never
 * appears in application logs.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingGlobalFilter implements WebFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingGlobalFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        InetSocketAddress remote = request.getRemoteAddress();
        InetSocketAddress local = request.getLocalAddress();
        String remoteIp = remote != null ? remote.getAddress().getHostAddress() : "?";
        int localPort = local != null ? local.getPort() : -1;
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        long start = System.currentTimeMillis();

        LOG.info("INBOUND {} {} from={} port={}", method, path, remoteIp, localPort);

        return chain.filter(exchange).doFinally(signal -> {
            int status = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : -1;
            long elapsed = System.currentTimeMillis() - start;
            LOG.info("OUTBOUND {} {} from={} port={} status={} elapsedMs={}",
                    method, path, remoteIp, localPort, status, elapsed);
        });
    }
}