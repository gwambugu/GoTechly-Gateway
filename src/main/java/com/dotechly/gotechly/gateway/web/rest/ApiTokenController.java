package com.dotechly.gotechly.gateway.web.rest;

import com.dotechly.gotechly.gateway.security.AuthoritiesConstants;
import com.dotechly.gotechly.gateway.security.jwt.TokenProvider;
import com.dotechly.gotechly.gateway.service.ApiClientService;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Exchanges an API key and secret for a JWT — the machine-to-machine counterpart of
 * {@link UserJWTController}, which authenticates humans against Active Directory.
 *
 * <p>The token is the same HS512 JWT every other caller presents, so nothing downstream needs to
 * know how it was obtained. It carries exactly one authority, {@code ROLE_API_CLIENT}: enough to
 * satisfy the {@code .authenticated()} rule on the payment APIs, never enough for the ADMIN or
 * PARTNER_* rules.
 *
 * <p>Every failure returns an identical {@code 401} with no detail. Telling a caller whether the
 * client id existed, was revoked, or simply had the wrong secret turns this endpoint into an oracle.
 */
@RestController
@RequestMapping("/api/auth")
public class ApiTokenController {

    private static final List<SimpleGrantedAuthority> API_CLIENT_AUTHORITIES =
        List.of(new SimpleGrantedAuthority(AuthoritiesConstants.API_CLIENT));

    private final ApiClientService apiClientService;
    private final TokenProvider tokenProvider;
    private final Duration tokenValidity;

    public ApiTokenController(
        ApiClientService apiClientService,
        TokenProvider tokenProvider,
        @Value("${gotechly.security.api-client.token-validity-in-seconds:900}") long tokenValiditySeconds
    ) {
        this.apiClientService = apiClientService;
        this.tokenProvider = tokenProvider;
        this.tokenValidity = Duration.ofSeconds(tokenValiditySeconds);
    }

    @PostMapping("/token")
    public Mono<ResponseEntity<ApiTokenResponse>> token(@RequestBody Mono<ApiTokenVMBody> body) {
        return body
            .flatMap(request -> apiClientService.verifyCredentials(request.clientId(), request.clientSecret()))
            .flatMap(client -> apiClientService.touchLastUsed(client).thenReturn(client))
            .map(client -> {
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    client.getClientId(), null, API_CLIENT_AUTHORITIES);
                // Deliberately shorter than a human session: a machine renews silently, and the
                // shorter the lifetime the smaller the window a leaked token is worth anything.
                String jwt = tokenProvider.createToken(authentication, tokenValidity);
                return ResponseEntity.ok(new ApiTokenResponse(jwt, tokenValidity.toSeconds()));
            })
            .defaultIfEmpty(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /** Request body. A record so the controller needs no separate mutable VM for two fields. */
    public record ApiTokenVMBody(String clientId, String clientSecret) {}

    /** OAuth2-shaped so standard HTTP clients can consume it without custom parsing. */
    public static class ApiTokenResponse {

        private final String accessToken;
        private final long expiresIn;

        ApiTokenResponse(String accessToken, long expiresIn) {
            this.accessToken = accessToken;
            this.expiresIn = expiresIn;
        }

        @JsonProperty("access_token")
        public String getAccessToken() {
            return accessToken;
        }

        @JsonProperty("token_type")
        public String getTokenType() {
            return "Bearer";
        }

        @JsonProperty("expires_in")
        public long getExpiresIn() {
            return expiresIn;
        }
    }
}
