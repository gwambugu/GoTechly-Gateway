package com.dotechly.gotechly.gateway.config;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.stereotype.Component;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class AdditionalPortsLifecycle implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(AdditionalPortsLifecycle.class);

    private final HttpHandler httpHandler;
    private final MultiPortProperties multiPortProperties;
    private final ServerProperties serverProperties;
    private final ResourceLoader resourceLoader = new DefaultResourceLoader();
    private final List<DisposableServer> servers = new ArrayList<>();
    private volatile boolean running = false;

    public AdditionalPortsLifecycle(
            HttpHandler httpHandler,
            MultiPortProperties multiPortProperties,
            ServerProperties serverProperties) {
        this.httpHandler = httpHandler;
        this.multiPortProperties = multiPortProperties;
        this.serverProperties = serverProperties;
    }

    @Override
    public void start() {
        ReactorHttpHandlerAdapter handlerAdapter = new ReactorHttpHandlerAdapter(httpHandler);

        // Bind admin webapp UI port
        MultiPortProperties.WebappConfig webappConfig = multiPortProperties.getWebapp();
        if (webappConfig != null && webappConfig.getPort() > 0) {
            try {
                HttpServer server = HttpServer.create()
                        .port(webappConfig.getPort())
                        .handle(handlerAdapter);

                boolean tlsEnabled = webappConfig.getTls() != null && webappConfig.getTls().isEnabled();
                if (tlsEnabled) {
                    SslContext sslContext = buildSslContext("webapp", webappConfig.getTls());
                    server = server.secure(spec -> spec.sslContext(sslContext));
                }

                DisposableServer bound = server.bindNow();
                servers.add(bound);
                LOG.info("Webapp UI port bound on port {} (tls={})", bound.port(), tlsEnabled);
            } catch (Exception e) {
                LOG.error("Failed to bind webapp UI port on {}: {}", webappConfig.getPort(), e.getMessage(), e);
            }
        }

        // Bind partner portal UI port
        MultiPortProperties.WebappPortalConfig partnerConfig = multiPortProperties.getWebappPartner();
        if (partnerConfig != null && partnerConfig.getPort() > 0) {
            try {
                HttpServer server = HttpServer.create()
                        .port(partnerConfig.getPort())
                        .handle(handlerAdapter);

                boolean tlsEnabled = partnerConfig.getTls() != null && partnerConfig.getTls().isEnabled();
                if (tlsEnabled) {
                    SslContext sslContext = buildSslContext("webapp-partner", partnerConfig.getTls());
                    server = server.secure(spec -> spec.sslContext(sslContext));
                }

                DisposableServer bound = server.bindNow();
                servers.add(bound);
                LOG.info("Partner portal UI port bound on port {} (tls={})", bound.port(), tlsEnabled);
            } catch (Exception e) {
                LOG.error("Failed to bind partner portal UI port on {}: {}", partnerConfig.getPort(), e.getMessage(), e);
            }
        }

        for (Map.Entry<String, MultiPortProperties.PortConfig> entry : multiPortProperties.getPorts().entrySet()) {
            String name = entry.getKey();
            MultiPortProperties.PortConfig config = entry.getValue();

            try {
                HttpServer server = HttpServer.create()
                        .port(config.getPort())
                        .handle(handlerAdapter);

                if (config.getTls() != null && config.getTls().isEnabled()) {
                    SslContext sslContext = buildSslContext(name, config.getTls());
                    server = server.secure(spec -> spec.sslContext(sslContext));
                }

                DisposableServer bound = server.bindNow();
                servers.add(bound);
                LOG.info("Additional gateway port '{}' bound on port {} (tls={})",
                        name, bound.port(),
                        config.getTls() != null && config.getTls().isEnabled());
            } catch (Exception e) {
                LOG.error("Failed to bind additional port '{}' on {}: {}", name, config.getPort(), e.getMessage(), e);
            }
        }
        running = true;
    }

    @Override
    public void stop() {
        for (DisposableServer server : servers) {
            try {
                server.disposeNow();
            } catch (Exception e) {
                LOG.warn("Error disposing additional port server: {}", e.getMessage());
            }
        }
        servers.clear();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        // Start after main web server, stop before it
        return Integer.MAX_VALUE - 1;
    }

    private SslContext buildSslContext(String portName, MultiPortProperties.TlsConfig tlsConfig) {
        try {
            String ksPath;
            String ksPassword;
            String ksType;

            if (tlsConfig.isUseServerDefault()) {
                var serverSsl = serverProperties.getSsl();
                if (serverSsl == null) {
                    throw new IllegalStateException(
                            "Port '" + portName + "' uses server-default TLS but server.ssl is not configured");
                }
                ksPath = serverSsl.getKeyStore();
                ksPassword = serverSsl.getKeyStorePassword();
                ksType = serverSsl.getKeyStoreType() != null ? serverSsl.getKeyStoreType() : "JKS";
            } else {
                ksPath = tlsConfig.getKeyStore();
                ksPassword = tlsConfig.getKeyStorePassword();
                ksType = tlsConfig.getKeyStoreType();
            }

            KeyStore ks = loadKeyStore(ksPath, ksPassword, ksType);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, ksPassword.toCharArray());

            SslContextBuilder builder = SslContextBuilder.forServer(kmf);

            List<String> protocols = tlsConfig.isUseServerDefault()
                    ? List.of("TLSv1.2")
                    : tlsConfig.getProtocols();
            if (protocols != null && !protocols.isEmpty()) {
                builder.protocols(protocols.toArray(String[]::new));
            }

            if (tlsConfig.getCiphers() != null && !tlsConfig.getCiphers().isEmpty()) {
                builder.ciphers(tlsConfig.getCiphers());
            }

            if (tlsConfig.getClientAuth() != MultiPortProperties.ClientAuth.NONE) {
                String tsPath = tlsConfig.getTrustStore();
                String tsPassword = tlsConfig.getTrustStorePassword();
                String tsType = tlsConfig.getTrustStoreType();

                if (tsPath == null || tsPath.isBlank()) {
                    throw new IllegalStateException(
                            "Port '" + portName + "' requires client-auth but no trust-store is configured");
                }

                KeyStore ts = loadKeyStore(tsPath, tsPassword, tsType);
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ts);
                builder.trustManager(tmf);
                builder.clientAuth(mapClientAuth(tlsConfig.getClientAuth()));
            }

            return builder.build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build SslContext for port '" + portName + "'", e);
        }
    }

    private KeyStore loadKeyStore(String path, String password, String type) throws Exception {
        Resource resource = resourceLoader.getResource(path);
        KeyStore ks = KeyStore.getInstance(type);
        try (InputStream is = resource.getInputStream()) {
            ks.load(is, password != null ? password.toCharArray() : null);
        }
        return ks;
    }

    private static ClientAuth mapClientAuth(MultiPortProperties.ClientAuth clientAuth) {
        return switch (clientAuth) {
            case NEED -> ClientAuth.REQUIRE;
            case WANT -> ClientAuth.OPTIONAL;
            case NONE -> ClientAuth.NONE;
        };
    }
}
