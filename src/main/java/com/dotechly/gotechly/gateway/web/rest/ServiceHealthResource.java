package com.dotechly.gotechly.gateway.web.rest;

import com.dotechly.gotechly.gateway.config.GatewayServicesProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/services")
public class ServiceHealthResource {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String DRAIN_PATH = "/api/v1/admin/node/drain";

    private final GatewayServicesProperties properties;
    private final WebClient webClient;

    public ServiceHealthResource(GatewayServicesProperties properties, WebClient insecureWebClient) {
        this.properties = properties;
        this.webClient = insecureWebClient;
    }

    @GetMapping
    public Flux<Map<String, Object>> getServiceHealth() {
        return Flux.fromIterable(properties.getServices())
                .flatMap(this::describe);
    }

    /**
     * Health and drain state are asked separately and merged, because they answer different
     * questions and must not be conflated.
     *
     * <p>{@code /actuator/health} says whether the instance is <em>alive</em>. It deliberately does
     * <b>not</b> move when an operator drains a node — otherwise a node taken out of the pool on
     * purpose would render on this page identically to one that has crashed, which is exactly the
     * distinction needed mid-deploy. So drain state is read from Core's own drain endpoint and
     * reported alongside, giving the page three states instead of two: UP, DRAINING, DOWN.
     */
    private Mono<Map<String, Object>> describe(GatewayServicesProperties.ServiceConfig service) {
        return Mono.zip(checkHealth(service), readDrain(service), (health, drain) -> {
            Map<String, Object> merged = new HashMap<>(health);
            merged.putAll(drain);
            return merged;
        });
    }

    private Mono<Map<String, Object>> checkHealth(GatewayServicesProperties.ServiceConfig service) {
        Instant start = Instant.now();
        return webClient.get()
                .uri(service.getBaseUrl() + service.getHealthPath())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(TIMEOUT)
                .map(body -> {
                    long ms = Duration.between(start, Instant.now()).toMillis();
                    return Map.<String, Object>of(
                            "name", service.getName(),
                            "baseUrl", service.getBaseUrl(),
                            "status", "UP",
                            "responseTimeMs", ms,
                            "lastChecked", Instant.now().toString()
                    );
                })
                .onErrorResume(ex -> {
                    long ms = Duration.between(start, Instant.now()).toMillis();
                    return Mono.just(Map.<String, Object>of(
                            "name", service.getName(),
                            "baseUrl", service.getBaseUrl(),
                            "status", "DOWN",
                            "responseTimeMs", ms,
                            "lastChecked", Instant.now().toString(),
                            "error", ex.getMessage() != null ? ex.getMessage() : "Unknown error"
                    ));
                });
    }

    /**
     * A node that cannot be asked reports {@code drainState: UNKNOWN} rather than defaulting to
     * ROUTABLE. An unreachable node is usually also DOWN, and claiming it is routable would put a
     * Drain button in front of an operator that cannot work.
     */
    private Mono<Map<String, Object>> readDrain(GatewayServicesProperties.ServiceConfig service) {
        return webClient.get()
                .uri(service.getBaseUrl() + DRAIN_PATH)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(TIMEOUT)
                .map(body -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> drain = (Map<String, Object>) body;
                    return Map.<String, Object>of(
                            "drainState", String.valueOf(drain.getOrDefault("state", "UNKNOWN")),
                            "inFlightHttp", drain.getOrDefault("inFlightHttp", 0),
                            "oldestRequestAgeMs", drain.getOrDefault("oldestRequestAgeMs", 0),
                            "activeBackgroundJobs", drain.getOrDefault("activeBackgroundJobs", List.of()),
                            "quiet", drain.getOrDefault("quiet", false),
                            // How long it has been in this state, and who put it there: the two
                            // facts that turn "one node is drained" into "and it has been for three
                            // hours, ask Alice".
                            "stateForSeconds", drain.getOrDefault("stateForSeconds", 0),
                            "requestedBy", String.valueOf(drain.getOrDefault("requestedBy", "unknown")),
                            // What is in flight and what arrived after the drain, with remote
                            // address — the only way to tell which route is still sending work
                            // to a node the load balancer should have stopped using.
                            "inFlightRequests", drain.getOrDefault("inFlightRequests", List.of()),
                            "arrivalsWhileDraining", drain.getOrDefault("arrivalsWhileDraining", 0),
                            "recentArrivalsWhileDraining",
                            drain.getOrDefault("recentArrivalsWhileDraining", List.of())
                    );
                })
                .onErrorResume(ex -> Mono.just(Map.<String, Object>of(
                        "drainState", "UNKNOWN",
                        "inFlightHttp", 0,
                        "oldestRequestAgeMs", 0,
                        "activeBackgroundJobs", List.of(),
                        "quiet", false,
                        "stateForSeconds", 0,
                        "requestedBy", "unknown",
                        "inFlightRequests", List.of(),
                        "arrivalsWhileDraining", 0,
                        "recentArrivalsWhileDraining", List.of()
                )));
    }
}
