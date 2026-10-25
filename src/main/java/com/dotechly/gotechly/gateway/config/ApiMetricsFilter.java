package com.dotechly.gotechly.gateway.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Component
public class ApiMetricsFilter implements GlobalFilter, Ordered {

    private final MeterRegistry meterRegistry;

    public ApiMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Instant start = Instant.now();
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String routeId = route != null ? route.getId() : "unknown";
            String method = exchange.getRequest().getMethod().name();
            int status = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value() : 0;
            Duration duration = Duration.between(start, Instant.now());

            Timer.builder("gateway.request.duration")
                    .tag("routeId", routeId)
                    .tag("method", method)
                    .tag("status", String.valueOf(status))
                    .register(meterRegistry)
                    .record(duration);

            meterRegistry.counter("gateway.request.count",
                    "routeId", routeId,
                    "method", method,
                    "status", String.valueOf(status))
                    .increment();
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
