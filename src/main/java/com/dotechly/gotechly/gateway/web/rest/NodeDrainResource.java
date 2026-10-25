package com.dotechly.gotechly.gateway.web.rest;

import com.dotechly.gotechly.gateway.config.GatewayServicesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.Duration;
import java.util.Map;

/**
 * Puts one Core instance into maintenance from the admin portal: it stops receiving new work while
 * finishing what it already accepted, so a plugin JAR can be swapped and the node restarted without
 * failing a live transaction.
 *
 * <p><b>Why this exists as a controller rather than a route.</b> Every Gateway route is
 * {@code uri: lb://gotechly-core} — by construction load balancing cannot address one instance. Drain is
 * inherently per-instance, so the call is made directly against
 * {@code gotechly.gateway.services[].base-url}, exactly as {@link ServiceHealthResource} already probes
 * each instance for the health page. That list is reused deliberately: the Gateway already keeps two
 * instance lists (the LB's discovery list and this one) and a third would be one more thing to drift.
 *
 * <p><b>Credentials stay on the Gateway.</b> {@code insecureWebClient} carries the mTLS client
 * certificate and the {@code X-Internal-Api-Key} header that Core requires
 * ({@code client-auth: need} + {@code InternalApiKeyFilter}). The operator authenticates once to the
 * portal — {@code /api/admin/**} is already {@code hasAuthority(ADMIN)} — and never handles either
 * credential. Without this endpoint an operator would have to curl Core directly, which means
 * copying the Gateway's client keystore and internal API key onto a laptop: a credential that can
 * call every internal Core API, handed out to do one thing.
 *
 * <p>See {@code docs/controlled-plugin-deployment.md}.
 */
@RestController
@RequestMapping("/api/admin/nodes")
public class NodeDrainResource {

    private static final Logger LOG = LoggerFactory.getLogger(NodeDrainResource.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String DRAIN_PATH = "/api/v1/admin/node/drain";
    private static final String UNDRAIN_PATH = "/api/v1/admin/node/undrain";
    private static final String ROUTABLE_PATH = "/api/v1/admin/node/routable";

    private final GatewayServicesProperties properties;
    private final WebClient webClient;

    public NodeDrainResource(GatewayServicesProperties properties, WebClient insecureWebClient) {
        this.properties = properties;
        this.webClient = insecureWebClient;
    }

    /**
     * POST /api/admin/nodes/{name}/drain
     *
     * <p>Refuses with <b>409</b> if this is the last instance still taking traffic. Draining all of
     * them leaves the load balancer with no servers, opens the circuit breaker, and sends every
     * request to {@code /api/fallback/core-unavailable} — a full outage produced by a maintenance
     * action. {@code ?force=true} is the deliberate full-stop escape hatch.
     */
    @PostMapping("/{name}/drain")
    public Mono<ResponseEntity<Map<String, Object>>> drain(@PathVariable String name,
                                                           @RequestParam(defaultValue = "false") boolean force,
                                                           ServerWebExchange exchange) {
        GatewayServicesProperties.ServiceConfig target = find(name);
        if (target == null) {
            return Mono.just(notFound(name));
        }
        return operator(exchange).flatMap(user -> {
            if (force) {
                LOG.warn("[NODE-DRAIN] {} forced the drain of '{}' — the last-routable-node guard was "
                        + "bypassed and the platform may now have no instance taking traffic", user, name);
                return send(target, DRAIN_PATH, name, user);
            }
            return countRoutablePeers(name).flatMap(peers -> {
                if (peers == 0) {
                    LOG.warn("[NODE-DRAIN] refused {}'s drain of '{}': it is the last instance taking traffic",
                            user, name);
                    return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).body(Map.<String, Object>of(
                            "error", "LAST_ROUTABLE_NODE",
                            "node", name,
                            "message", "'" + name + "' is the only instance still taking traffic. Draining it "
                                    + "would leave the load balancer with no servers and return 503 for every "
                                    + "request. Bring another instance up first, or repeat with ?force=true if "
                                    + "a full stop is intended.")));
                }
                return send(target, DRAIN_PATH, name, user);
            });
        });
    }

    /** POST /api/admin/nodes/{name}/undrain — put the instance back into the pool. Never guarded. */
    @PostMapping("/{name}/undrain")
    public Mono<ResponseEntity<Map<String, Object>>> undrain(@PathVariable String name,
                                                             ServerWebExchange exchange) {
        GatewayServicesProperties.ServiceConfig target = find(name);
        if (target == null) {
            return Mono.just(notFound(name));
        }
        return operator(exchange).flatMap(user -> send(target, UNDRAIN_PATH, name, user));
    }

    /**
     * Who is doing this. The Gateway is the only party that knows — Core sees an mTLS client
     * certificate and an internal API key, both of which identify the <em>Gateway</em>, not the
     * person. It is forwarded so the node's own log can answer "who drained this and are they
     * finished", which is the first question asked when one is found still drained hours later.
     */
    private Mono<String> operator(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .map(Principal::getName)
                .filter(name -> !name.isBlank())
                .defaultIfEmpty("unknown");
    }

    // =========================================================================

    /**
     * How many instances *other than* {@code excluding} are currently answering the load balancer's
     * probe with 200. Asks the same endpoint the LB itself asks, so the count reflects what the LB
     * will actually see rather than a drain flag alone — an instance whose database is down is
     * already out of the pool and must not be counted as cover.
     *
     * <p>An unreachable instance counts as not routable: when in doubt, refuse the drain. The wrong
     * answer in the safe direction costs an operator one `?force=true`; in the other direction it
     * costs an outage.
     */
    private Mono<Long> countRoutablePeers(String excluding) {
        return Flux.fromIterable(properties.getServices())
                .filter(s -> !s.getName().equals(excluding))
                .flatMap(s -> webClient.get()
                        .uri(s.getBaseUrl() + ROUTABLE_PATH)
                        .exchangeToMono(response -> Mono.just(response.statusCode().is2xxSuccessful()))
                        .timeout(TIMEOUT)
                        .onErrorResume(ex -> {
                            LOG.warn("[NODE-DRAIN] peer '{}' unreachable while checking cover: {}",
                                    s.getName(), ex.getMessage());
                            return Mono.just(false);
                        }))
                .filter(Boolean::booleanValue)
                .count();
    }

    private Mono<ResponseEntity<Map<String, Object>>> send(GatewayServicesProperties.ServiceConfig target,
                                                           String path, String name, String user) {
        return webClient.post()
                .uri(target.getBaseUrl() + path)
                .header("X-Requested-By", user)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(TIMEOUT)
                .map(body -> {
                    LOG.info("[NODE-DRAIN] {} on '{}' -> {}", path, name, body);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typed = (Map<String, Object>) body;
                    return ResponseEntity.ok(Map.<String, Object>of("node", name, "result", typed));
                })
                .onErrorResume(ex -> {
                    LOG.error("[NODE-DRAIN] {} on '{}' failed: {}", path, name, ex.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.<String, Object>of(
                            "error", "NODE_UNREACHABLE",
                            "node", name,
                            "message", "Could not reach " + target.getBaseUrl() + ": "
                                    + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()))));
                });
    }

    private GatewayServicesProperties.ServiceConfig find(String name) {
        return properties.getServices().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private static ResponseEntity<Map<String, Object>> notFound(String name) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "UNKNOWN_NODE",
                "node", name,
                "message", "No instance named '" + name + "' in gotechly.gateway.services."));
    }
}
