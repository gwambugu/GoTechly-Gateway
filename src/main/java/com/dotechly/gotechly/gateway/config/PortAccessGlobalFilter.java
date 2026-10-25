package com.dotechly.gotechly.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PortAccessGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(PortAccessGlobalFilter.class);

    private final int mainServerPort;
    private final int webappPort;
    private final int webappPartnerPort;
    private final Map<Integer, List<PathPattern>> portPatternCache;
    private final Map<Integer, IpAllowlist> portIpCache;

    public PortAccessGlobalFilter(
            MultiPortProperties properties,
            @Value("${server.port:8080}") int mainServerPort) {
        this.mainServerPort = mainServerPort;
        this.webappPort = properties.getWebappPort();
        this.webappPartnerPort = properties.getWebappPartnerPort();
        this.portPatternCache = buildPatternCache(properties);
        this.portIpCache = buildIpCache(properties);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        InetSocketAddress localAddress = exchange.getRequest().getLocalAddress();
        if (localAddress == null) {
            return chain.filter(exchange);
        }

        int localPort = localAddress.getPort();

        // Main server port: allow all Gateway routes
        if (localPort == mainServerPort) {
            return chain.filter(exchange);
        }

        // Admin webapp port: allow portal APIs, block payment APIs
        if (localPort == webappPort) {
            String path = exchange.getRequest().getURI().getPath();
            if (path.startsWith("/services/core/payment/")) {
                LOG.info("Payment path {} blocked on webapp port {}", path, webappPort);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        }

        // Partner webapp port: enforce allowed-paths (partner, transactions, audit only)
        if (localPort == webappPartnerPort) {
            List<PathPattern> patterns = portPatternCache.get(webappPartnerPort);
            if (patterns != null) {
                String path = exchange.getRequest().getURI().getPath();
                PathContainer pathContainer = PathContainer.parsePath(path);
                boolean allowed = patterns.stream().anyMatch(p -> p.matches(pathContainer));
                if (!allowed) {
                    LOG.info("Path {} blocked on partner portal port {}", path, webappPartnerPort);
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
            }
            return chain.filter(exchange);
        }

        // Additional port: enforce IP allowlist (if configured) then allowed-paths
        List<PathPattern> patterns = portPatternCache.get(localPort);
        if (patterns == null) {
            LOG.warn("Request on unconfigured port {}, rejecting", localPort);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        IpAllowlist ipAllowlist = portIpCache.get(localPort);
        if (ipAllowlist != null && !ipAllowlist.allowed.isEmpty()) {
            String clientIp = resolveClientIp(exchange, ipAllowlist);
            if (clientIp == null || !matches(clientIp, ipAllowlist.allowed)) {
                LOG.info("IP {} blocked on port {} (remote={}, xff={})",
                        clientIp,
                        localPort,
                        remoteAddress(exchange),
                        exchange.getRequest().getHeaders().getFirst("X-Forwarded-For"));
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }

        String path = exchange.getRequest().getURI().getPath();
        PathContainer pathContainer = PathContainer.parsePath(path);
        boolean allowed = patterns.stream().anyMatch(p -> p.matches(pathContainer));

        if (!allowed) {
            LOG.info("Path {} blocked on port {}", path, localPort);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // Run after logging/metrics filters, before rate limiter and routing
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }

    /**
     * Returns the effective client IP for allowlist evaluation.
     * <p>
     * If trustedProxies is empty, the immediate TCP peer is the client (direct exposure — UAT).
     * If trustedProxies is non-empty AND the immediate peer matches one, walk X-Forwarded-For
     * right-to-left skipping trusted hops; the first untrusted hop is the real client (F5 PROD).
     * If the immediate peer is NOT a trusted proxy, ignore X-Forwarded-For (forgeable from the
     * Internet) and use the peer address.
     */
    private String resolveClientIp(ServerWebExchange exchange, IpAllowlist allowlist) {
        String remote = remoteAddress(exchange);
        if (remote == null) {
            return null;
        }
        if (allowlist.trustedProxies.isEmpty() || !matches(remote, allowlist.trustedProxies)) {
            return remote;
        }
        String xff = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xff == null || xff.isBlank()) {
            return remote;
        }
        String[] hops = xff.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (hop.isEmpty()) continue;
            if (!matches(hop, allowlist.trustedProxies)) {
                return hop;
            }
        }
        // Every hop in XFF was a trusted proxy — fall back to the peer (also trusted, so will fail allowlist below).
        return remote;
    }

    private static String remoteAddress(ServerWebExchange exchange) {
        InetSocketAddress addr = exchange.getRequest().getRemoteAddress();
        return addr == null ? null : addr.getAddress().getHostAddress();
    }

    private static boolean matches(String ip, List<CidrMatcher> matchers) {
        for (CidrMatcher m : matchers) {
            if (m.matches(ip)) return true;
        }
        return false;
    }

    private static Map<Integer, List<PathPattern>> buildPatternCache(MultiPortProperties properties) {
        PathPatternParser parser = new PathPatternParser();
        Map<Integer, List<PathPattern>> cache = new HashMap<>();

        // Register additional named ports
        properties.getPorts().forEach((name, config) -> {
            List<PathPattern> patterns = config.getAllowedPaths().stream()
                    .map(parser::parse)
                    .toList();
            cache.put(config.getPort(), patterns);
            LOG.info("Port '{}' ({}) allows paths: {}", name, config.getPort(), config.getAllowedPaths());
        });

        // Register partner portal port patterns
        MultiPortProperties.WebappPortalConfig partnerConfig = properties.getWebappPartner();
        if (partnerConfig != null && partnerConfig.getPort() > 0 && partnerConfig.getAllowedPaths() != null) {
            List<PathPattern> patterns = partnerConfig.getAllowedPaths().stream()
                    .map(parser::parse)
                    .toList();
            cache.put(partnerConfig.getPort(), patterns);
            LOG.info("Port 'partner-portal' ({}) allows paths: {}", partnerConfig.getPort(), partnerConfig.getAllowedPaths());
        }

        return cache;
    }

    private static Map<Integer, IpAllowlist> buildIpCache(MultiPortProperties properties) {
        Map<Integer, IpAllowlist> cache = new HashMap<>();
        properties.getPorts().forEach((name, config) -> {
            List<CidrMatcher> allowed = compileMatchers(config.getAllowedIps(), name, "allowed-ips");
            List<CidrMatcher> proxies = compileMatchers(config.getTrustedProxies(), name, "trusted-proxies");
            if (!allowed.isEmpty() || !proxies.isEmpty()) {
                cache.put(config.getPort(), new IpAllowlist(allowed, proxies));
                LOG.info("Port '{}' ({}) IP allowlist: allowed={} trustedProxies={}",
                        name, config.getPort(), config.getAllowedIps(), config.getTrustedProxies());
            }
        });
        return cache;
    }

    private static List<CidrMatcher> compileMatchers(List<String> cidrs, String portName, String field) {
        if (cidrs == null) return List.of();
        List<CidrMatcher> out = new ArrayList<>(cidrs.size());
        for (String cidr : cidrs) {
            if (cidr == null || cidr.isBlank()) continue;
            try {
                out.add(new CidrMatcher(cidr.trim()));
            } catch (IllegalArgumentException e) {
                LOG.error("Port '{}' has invalid CIDR '{}' in {} — entry skipped: {}", portName, cidr, field, e.getMessage());
            }
        }
        return out;
    }

    private record IpAllowlist(List<CidrMatcher> allowed, List<CidrMatcher> trustedProxies) {}

    /**
     * IPv4/IPv6 CIDR matcher with no servlet-API dependency (Spring Security's IpAddressMatcher
     * has an HttpServletRequest overload that breaks compilation on this WebFlux-only module).
     */
    static final class CidrMatcher {
        private final byte[] network;
        private final int prefix;

        CidrMatcher(String spec) {
            int slash = spec.indexOf('/');
            String host = slash < 0 ? spec : spec.substring(0, slash);
            InetAddress addr;
            try {
                addr = InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Invalid IP/CIDR: " + spec, e);
            }
            this.network = addr.getAddress();
            int maxBits = network.length * 8;
            int parsed;
            if (slash < 0) {
                parsed = maxBits;
            } else {
                try {
                    parsed = Integer.parseInt(spec.substring(slash + 1));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid CIDR prefix: " + spec, e);
                }
            }
            if (parsed < 0 || parsed > maxBits) {
                throw new IllegalArgumentException("Prefix out of range for address family: " + spec);
            }
            this.prefix = parsed;
        }

        boolean matches(String candidate) {
            if (candidate == null) return false;
            InetAddress c;
            try {
                c = InetAddress.getByName(candidate);
            } catch (UnknownHostException e) {
                return false;
            }
            byte[] addr = c.getAddress();
            if (addr.length != network.length) return false; // IPv4 vs IPv6 mismatch
            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (addr[i] != network[i]) return false;
            }
            if (remainingBits == 0) return true;
            int mask = (0xFF << (8 - remainingBits)) & 0xFF;
            return (addr[fullBytes] & mask) == (network[fullBytes] & mask);
        }
    }
}
