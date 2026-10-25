package com.dotechly.gotechly.gateway.web.rest;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/fallback")
public class FallbackResource {

    /**
     * Default fallback for any upstream CircuitBreaker trip.
     */
    @RequestMapping
    public Mono<ResponseEntity<Map<String, Object>>> fallback() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "error", "Service Unavailable",
                        "message", "The upstream service is temporarily unavailable. Please try again later."
                )));
    }

    /**
     * Specific fallback for GoTechly-Core CircuitBreaker trips.
     * Referenced from {@code spring.cloud.gateway.server.webflux.routes[*].filters.CircuitBreaker.fallbackUri}.
     */
    @RequestMapping("/core-unavailable")
    public Mono<ResponseEntity<Map<String, Object>>> coreUnavailable() {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "error", "Service Unavailable",
                        "message", "All GoTechly-Core instances are unavailable. Try again shortly."
                )));
    }
}
