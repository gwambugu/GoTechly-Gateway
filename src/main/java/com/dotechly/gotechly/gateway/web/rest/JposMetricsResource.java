package com.dotechly.gotechly.gateway.web.rest;

import com.dotechly.gotechly.gateway.config.GatewayServicesProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

/**
 * Gateway endpoint that aggregates jPOS TransactionManager metrics
 * from all Core instances and proxies scaling commands to specific instances.
 */
@RestController
@RequestMapping("/api/admin/jpos")
public class JposMetricsResource {

    private static final String JPOS_METRICS_PATH = "/api/v1/admin/jpos/metrics";
    private static final String JPOS_SCALE_SESSIONS_PATH = "/api/v1/admin/jpos/scale/sessions";
    private static final String JPOS_SCALE_TM_COUNT_PATH = "/api/v1/admin/jpos/scale/tm-count";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final GatewayServicesProperties properties;
    private final WebClient webClient;

    public JposMetricsResource(GatewayServicesProperties properties, WebClient insecureWebClient) {
        this.properties = properties;
        this.webClient = insecureWebClient;
    }

    /**
     * GET /api/admin/jpos/metrics
     * Fetches jPOS metrics from ALL Core instances and returns them grouped by instance.
     */
    @GetMapping("/metrics")
    public Flux<Map<String, Object>> getAllInstanceMetrics() {
        return Flux.fromIterable(properties.getServices())
                .flatMap(service -> fetchMetrics(service)
                        .map(metrics -> {
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("instance", service.getName());
                            result.put("baseUrl", service.getBaseUrl());
                            result.putAll(metrics);
                            return result;
                        })
                        .onErrorResume(ex -> Mono.just(Map.of(
                                "instance", service.getName(),
                                "baseUrl", service.getBaseUrl(),
                                "status", "UNREACHABLE",
                                "error", ex.getMessage() != null ? ex.getMessage() : "Unknown error"
                        )))
                );
    }

    /**
     * POST /api/admin/jpos/scale/sessions
     * Proxies session scaling to a specific Core instance or all instances.
     * <p>
     * Body: {@code {"instance": "GoTechly-Core-1", "tmName": "mpesaTransactionManager_1", "sessions": 15, "maxSessions": 30}}
     * <p>
     * If {@code instance} is omitted or {@code "all"}, the command is sent to all instances.
     */
    @PostMapping("/scale/sessions")
    public Flux<Map<String, Object>> scaleSessions(@RequestBody Map<String, Object> request) {
        String targetInstance = (String) request.get("instance");
        return proxyScaleCommand(targetInstance, JPOS_SCALE_SESSIONS_PATH, request);
    }

    /**
     * POST /api/admin/jpos/scale/tm-count
     * Proxies TM count scaling to a specific Core instance or all instances.
     * <p>
     * Body: {@code {"instance": "GoTechly-Core-1", "prefix": "mpesa", "count": 4}}
     * <p>
     * If {@code instance} is omitted or {@code "all"}, the command is sent to all instances.
     */
    @PostMapping("/scale/tm-count")
    public Flux<Map<String, Object>> scaleTmCount(@RequestBody Map<String, Object> request) {
        String targetInstance = (String) request.get("instance");
        return proxyScaleCommand(targetInstance, JPOS_SCALE_TM_COUNT_PATH, request);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Mono<Map<String, Object>> fetchMetrics(GatewayServicesProperties.ServiceConfig service) {
        return webClient.get()
                .uri(service.getBaseUrl() + JPOS_METRICS_PATH)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .timeout(TIMEOUT);
    }

    private Flux<Map<String, Object>> proxyScaleCommand(String targetInstance, String path,
            Map<String, Object> request) {

        List<GatewayServicesProperties.ServiceConfig> targets;
        if (targetInstance == null || targetInstance.isBlank() || "all".equalsIgnoreCase(targetInstance)) {
            targets = properties.getServices();
        } else {
            targets = properties.getServices().stream()
                    .filter(s -> targetInstance.equals(s.getName()))
                    .toList();
            if (targets.isEmpty()) {
                return Flux.just(Map.of("error",
                        "Instance '" + targetInstance + "' not found. Available: " +
                                properties.getServices().stream()
                                        .map(GatewayServicesProperties.ServiceConfig::getName)
                                        .toList()));
            }
        }

        return Flux.fromIterable(targets)
                .flatMap(service -> webClient.post()
                        .uri(service.getBaseUrl() + path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .timeout(TIMEOUT)
                        .map(response -> {
                            Map<String, Object> result = new LinkedHashMap<>();
                            result.put("instance", service.getName());
                            result.putAll(response);
                            return result;
                        })
                        .onErrorResume(ex -> Mono.just(Map.of(
                                "instance", service.getName(),
                                "status", "FAILED",
                                "error", ex.getMessage() != null ? ex.getMessage() : "Unknown error"
                        )))
                );
    }
}
