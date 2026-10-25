package com.dotechly.gotechly.gateway.security.jwt;

import com.dotechly.gotechly.gateway.security.AuthoritiesConstants;
import com.dotechly.gotechly.gateway.service.ApiClientStatusCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filters incoming requests and installs a Spring Security principal if a header corresponding to a valid user is
 * found.
 *
 * <p>Machine tokens get one extra check. A JWT stays valid until it expires, so revoking an API
 * client would not stop the tokens it already holds; for {@code ROLE_API_CLIENT} tokens the filter
 * asks {@link ApiClientStatusCache} whether the credential is still live and installs no principal
 * when it is not — the request then falls through to {@code authorizeExchange} and is rejected.
 */
public class JWTFilter implements WebFilter {

    private final Logger log = LoggerFactory.getLogger(JWTFilter.class);

    public static final String AUTHORIZATION_HEADER = "Authorization";

    private final TokenProvider tokenProvider;

    private final ApiClientStatusCache apiClientStatusCache;

    public JWTFilter(TokenProvider tokenProvider, ApiClientStatusCache apiClientStatusCache) {
        this.tokenProvider = tokenProvider;
        this.apiClientStatusCache = apiClientStatusCache;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String jwt = resolveToken(exchange.getRequest());
        if (StringUtils.hasText(jwt) && this.tokenProvider.validateToken(jwt)) {
            Authentication authentication = this.tokenProvider.getAuthentication(jwt);
            if (isApiClient(authentication)) {
                return apiClientStatusCache.isActive(authentication.getName())
                    .flatMap(active -> {
                        if (!active) {
                            log.warn("Rejecting revoked/expired API client token: clientId={}, path={}",
                                authentication.getName(), exchange.getRequest().getPath());
                            // No principal installed — authorizeExchange turns this into a 401.
                            return chain.filter(exchange);
                        }
                        return authenticate(exchange, chain, authentication);
                    });
            }
            return authenticate(exchange, chain, authentication);
        }
        return chain.filter(exchange);
    }

    private Mono<Void> authenticate(ServerWebExchange exchange, WebFilterChain chain, Authentication authentication) {
        log.debug("JWT authenticated user: {}, authorities: {}, path: {}",
            authentication.getName(), authentication.getAuthorities(), exchange.getRequest().getPath());
        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(r -> r.header("X-Auth-User", authentication.getName()))
            .build();
        return chain.filter(mutatedExchange)
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private boolean isApiClient(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(AuthoritiesConstants.API_CLIENT::equals);
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
