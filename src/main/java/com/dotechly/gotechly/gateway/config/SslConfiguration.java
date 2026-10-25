package com.dotechly.gotechly.gateway.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

/**
 * Provides a WebClient for internal calls to Core (health checks, metrics, jPOS status).
 * Uses InsecureTrustManagerFactory (dev — accepts self-signed/hostname-mismatched certs)
 * AND presents the mTLS client certificate so Core's {@code client-auth: need} is satisfied.
 * <p>
 * IMPORTANT: Do NOT expose {@code HttpClient} as a {@code @Bean} — Spring Cloud Gateway's
 * auto-configuration uses {@code @ConditionalOnMissingBean(HttpClient.class)}, so any custom
 * {@code HttpClient} bean would replace the auto-configured proxy client.
 */
@Configuration
public class SslConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SslConfiguration.class);

    @Bean
    public WebClient insecureWebClient(
            @Value("${spring.cloud.gateway.server.webflux.httpclient.ssl.key-store:}") String keyStorePath,
            @Value("${spring.cloud.gateway.server.webflux.httpclient.ssl.key-store-password:}") String keyStorePassword,
            @Value("${spring.cloud.gateway.server.webflux.httpclient.ssl.key-store-type:PKCS12}") String keyStoreType,
            @Value("${gotechly.security.internal-api-key.secret}") String apiKey,
            @Value("${gotechly.security.internal-api-key.header-name:X-Internal-Api-Key}") String apiKeyHeader
    ) throws Exception {

        SslContextBuilder builder = SslContextBuilder.forClient()
                .protocols("TLSv1.2")
                .trustManager(InsecureTrustManagerFactory.INSTANCE);

        // Present mTLS client cert so Core accepts the connection
        if (keyStorePath != null && !keyStorePath.isBlank()) {
            Resource resource = new DefaultResourceLoader().getResource(keyStorePath);
            KeyStore ks = KeyStore.getInstance(keyStoreType);
            try (InputStream is = resource.getInputStream()) {
                ks.load(is, keyStorePassword.toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, keyStorePassword.toCharArray());
            builder.keyManager(kmf);
            LOG.info("Internal WebClient initialized with mTLS client cert from {}", keyStorePath);
        } else {
            LOG.warn("Internal WebClient has no client cert — Core mTLS calls will fail");
        }

        SslContext sslContext = builder.build();
        HttpClient httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(sslContext));
        httpClient.warmup().block();
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(apiKeyHeader, apiKey)
                .build();
    }
}
