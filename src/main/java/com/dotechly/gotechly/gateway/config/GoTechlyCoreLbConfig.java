package com.dotechly.gotechly.gateway.config;

import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Per-client LoadBalancer configuration for {@code gotechly-core}. Registered via
 * {@code @LoadBalancerClient(name = "gotechly-core", configuration = ...)} on the
 * application class, so this class must NOT be {@code @Configuration} (it's
 * instantiated in a child context, not scanned).
 * <p>
 * Wires the existing {@code insecureWebClient} (mTLS client cert + X-Internal-Api-Key
 * header) into {@link ServiceInstanceListSupplier#builder()}'s health-check stage so
 * the LB can successfully probe Core's {@code /actuator/health}. Without this, the
 * default health-check client has no mTLS cert and every TLS handshake fails, causing
 * the LB to filter out *all* instances even when one is up.
 */
public class GoTechlyCoreLbConfig {

    @Bean
    public ServiceInstanceListSupplier gotechlyCoreInstanceListSupplier(
            ConfigurableApplicationContext context,
            WebClient insecureWebClient
    ) {
        return ServiceInstanceListSupplier.builder()
                .withDiscoveryClient()
                .withHealthChecks(insecureWebClient)
                .withCaching()
                .build(context);
    }
}