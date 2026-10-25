package com.dotechly.gotechly.gateway.config;


import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Reports how much of the Core cluster is actually carrying traffic.
 *
 * <p><b>Alive is not the same as taking traffic.</b> A node an operator has drained for maintenance
 * still answers {@code /actuator/health} with 200 — deliberately, so that a planned drain never
 * looks like a crash. The consequence is that counting healthy instances alone reports {@code 3/3}
 * while one of them is out of the load-balancer pool.
 *
 * <p>That gap is how a forgotten drain hides. Nothing fails: the remaining instances serve
 * everything, latency looks normal, and the cluster quietly runs at reduced capacity until peak load
 * arrives or a second instance dies. So the drain state is probed too and folded in here — the one
 * place existing monitoring already watches — rather than left as a log line nobody reads.
 *
 * <p>A drained instance keeps the overall status {@code UP}: the Gateway is fine and the platform is
 * serving. It is the <b>detail</b> that must say so, e.g. {@code 2/3 taking traffic (1 drained)}.
 * Reporting DOWN for a planned maintenance action would train people to ignore this indicator.
 */
@Component
public class CoreHealthIndicator implements ReactiveHealthIndicator {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String ROUTABLE_PATH = "/api/v1/admin/node/routable";

    private final List<WebClient> coreClients;

    public CoreHealthIndicator(GatewayServicesProperties properties, WebClient insecureWebClient) {
        this.coreClients = properties.getServices().stream()
                .filter(s -> s.getName().startsWith("GoTechly-Core"))
                .map(s -> insecureWebClient.mutate().baseUrl(s.getBaseUrl()).build())
                .toList();
    }

    @Override
    public Mono<Health> health() {
        return Flux.fromIterable(coreClients)
                .flatMap(this::probe)
                .collectList()
                .map(results -> {
                    long total = results.size();
                    long alive = results.stream().filter(r -> r != InstanceState.UNREACHABLE).count();
                    long drained = results.stream().filter(r -> r == InstanceState.DRAINED).count();
                    long taking = alive - drained;

                    if (alive == 0) {
                        return Health.down().withDetail("instances", "0/" + total + " healthy").build();
                    }

                    String detail = taking + "/" + total + " taking traffic";
                    if (drained > 0) {
                        detail += " (" + drained + " drained for maintenance)";
                    } else if (alive < total) {
                        detail += " (" + (total - alive) + " unreachable)";
                    }

                    // Everything drained is still UP — the Gateway works — but it is the state where
                    // every request falls through to the circuit-breaker fallback, so say it plainly.
                    if (taking == 0) {
                        detail += " — NO INSTANCE IS TAKING TRAFFIC";
                    }
                    return Health.up().withDetail("instances", detail).build();
                });
    }

    /**
     * One probe answers both questions: {@code /api/v1/admin/node/routable} returns 200 when the
     * instance is in the pool and 503 when it is drained (or its own health is not UP), so a
     * response at all means alive and the status code separates the two.
     */
    private Mono<InstanceState> probe(WebClient client) {
        return client.get()
                .uri(ROUTABLE_PATH)
                .exchangeToMono(response -> Mono.just(
                        response.statusCode().is2xxSuccessful() ? InstanceState.TAKING_TRAFFIC : InstanceState.DRAINED))
                .timeout(TIMEOUT)
                .onErrorReturn(InstanceState.UNREACHABLE);
    }

    private enum InstanceState {
        TAKING_TRAFFIC,
        /** Alive and answering, but out of the pool — drained, or failing its own health check. */
        DRAINED,
        UNREACHABLE
    }
}