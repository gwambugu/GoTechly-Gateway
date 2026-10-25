package com.dotechly.gotechly.gateway.config;

import com.dotechly.gotechly.gateway.security.AuthoritiesConstants;
import com.dotechly.gotechly.gateway.security.jwt.JWTFilter;
import com.dotechly.gotechly.gateway.security.jwt.TokenProvider;
import com.dotechly.gotechly.gateway.service.ApiClientStatusCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfiguration {

    private final TokenProvider tokenProvider;
    private final ReactiveUserDetailsService userDetailsService;
    private final ApiClientStatusCache apiClientStatusCache;

    public SecurityConfiguration(
            TokenProvider tokenProvider,
            ReactiveUserDetailsService userDetailsService,
            ApiClientStatusCache apiClientStatusCache
    ) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.apiClientStatusCache = apiClientStatusCache;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Local BCrypt authentication. Used only when AD is disabled
     * ({@code gotechly.security.ad.enabled} unset/false) — e.g. local dev without a domain
     * controller. When AD is enabled, {@code AdReactiveAuthenticationManager} is the active
     * {@link ReactiveAuthenticationManager} instead.
     */
    @Bean
    @ConditionalOnProperty(prefix = "gotechly.security.ad", name = "enabled", havingValue = "false", matchIfMissing = true)
    public ReactiveAuthenticationManager reactiveAuthenticationManager() {
        UserDetailsRepositoryReactiveAuthenticationManager authenticationManager = new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
        authenticationManager.setPasswordEncoder(passwordEncoder());
        return authenticationManager;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .securityMatcher(ServerWebExchangeMatchers.pathMatchers("/**"))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // Without this, ExceptionTranslationWebFilter's default entry point is
                // HttpBasicServerAuthenticationEntryPoint, which answers every 401 with
                // "WWW-Authenticate: Basic realm=..." — the browser then pops its own
                // credentials dialog on session expiry instead of letting the SPA redirect
                // to /login. httpBasic().disable() does NOT cover this: it only removes the
                // inbound filter that reads the header, never the entry point that writes it.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)))
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .addFilterBefore(new JWTFilter(tokenProvider, apiClientStatusCache), SecurityWebFiltersOrder.AUTHENTICATION)
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.mode(XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN))
                )
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/").permitAll()
                        .pathMatchers("/*.*").permitAll()
                        .pathMatchers("/api/register").permitAll()
                        .pathMatchers("/api/register/**").permitAll()
                        .pathMatchers("/api/activate").permitAll()
                        .pathMatchers("/api/authenticate").permitAll()
                        // Machine-to-machine token exchange. A caller presenting an API key has no
                        // token yet, so the endpoint that issues one cannot itself require a token.
                        // MUST stay above the /api/** catch-all — first match wins.
                        .pathMatchers(HttpMethod.POST, "/api/auth/token").permitAll()
                        .pathMatchers("/api/account/reset-password/init").permitAll()
                        .pathMatchers("/api/account/reset-password/finish").permitAll()
                        .pathMatchers("/api/portal-context").permitAll()
                        // Auth-mode probe read by the login/register pages before any token
                        // exists. MUST stay above the /api/** catch-all — first match wins.
                        .pathMatchers("/api/security-config").permitAll()
                        .pathMatchers("/api/fallback").permitAll()
                        .pathMatchers("/services/core/payment/api/v1/b2c/reactive/result/**").permitAll()
                        // Daraja result-webhooks: third party can't hold our JWT. Boundary is the
                        // dedicated daraja-callback multi-port listener (allowed-ips). MUST stay
                        // above the /services/core/payment/api/** catch-all — first match wins.
                        .pathMatchers("/services/core/payment/api/v1/mpesa/inquiry/callback/**").permitAll()
                        .pathMatchers("/services/core/payment/api/v1/legacy/**").permitAll()//https://10.11.6.111:3130
                        .pathMatchers("/services/core/api/v2/rndps/inward/callbacks/fsp/**").permitAll()
                        .pathMatchers("/services/core/payment/api/v1/accounts/linkage/callback/**").permitAll()
                        .pathMatchers("/services/core/api/v1/admin/**").hasAuthority(AuthoritiesConstants.ADMIN)
                        .pathMatchers(HttpMethod.POST, "/services/core/api/v1/partner/partners").hasAnyAuthority(AuthoritiesConstants.PARTNER_MAKER, AuthoritiesConstants.ADMIN)
                        .pathMatchers(HttpMethod.PUT, "/services/core/api/v1/partner/partners/**").hasAnyAuthority(AuthoritiesConstants.PARTNER_MAKER, AuthoritiesConstants.ADMIN)
                        .pathMatchers(HttpMethod.DELETE, "/services/core/api/v1/partner/partners/**").hasAuthority(AuthoritiesConstants.ADMIN)
                        .pathMatchers(HttpMethod.POST, "/services/core/api/v1/partner/partners/*/approve").hasAnyAuthority(AuthoritiesConstants.PARTNER_CHECKER, AuthoritiesConstants.ADMIN)
                        .pathMatchers(HttpMethod.POST, "/services/core/api/v1/partner/partners/*/reject").hasAnyAuthority(AuthoritiesConstants.PARTNER_CHECKER, AuthoritiesConstants.ADMIN)
                        .pathMatchers(HttpMethod.POST, "/services/core/api/v1/partner/partners/*/suspend").hasAnyAuthority(AuthoritiesConstants.PARTNER_CHECKER, AuthoritiesConstants.ADMIN)
                        .pathMatchers(HttpMethod.POST, "/services/core/api/v1/partner/partners/*/activate").hasAnyAuthority(AuthoritiesConstants.PARTNER_CHECKER, AuthoritiesConstants.ADMIN)
                        .pathMatchers(HttpMethod.GET, "/services/core/api/v1/partner/**").hasAnyAuthority(AuthoritiesConstants.PARTNER_VIEWER, AuthoritiesConstants.PARTNER_MAKER, AuthoritiesConstants.PARTNER_CHECKER, AuthoritiesConstants.ADMIN)
                        .pathMatchers(HttpMethod.POST, "/services/core/api/v1/partner/**").hasAnyAuthority(AuthoritiesConstants.PARTNER_MAKER, AuthoritiesConstants.ADMIN)
                        .pathMatchers(HttpMethod.PUT, "/services/core/api/v1/partner/**").hasAnyAuthority(AuthoritiesConstants.PARTNER_MAKER, AuthoritiesConstants.ADMIN)
                        .pathMatchers(HttpMethod.DELETE, "/services/core/api/v1/partner/**").hasAnyAuthority(AuthoritiesConstants.PARTNER_MAKER, AuthoritiesConstants.ADMIN)
                        .pathMatchers("/services/core/payment/api/**").authenticated()
                        .pathMatchers("/services/core/api/**").authenticated()
                        .pathMatchers("/services/core/actuator/**").hasAuthority(AuthoritiesConstants.ADMIN)
                        .pathMatchers("/api/admin/**").hasAuthority(AuthoritiesConstants.ADMIN)
                        .pathMatchers("/api/**").authenticated()
                        .pathMatchers("/v3/api-docs/**").permitAll()
                        .pathMatchers("/management/health").permitAll()
                        .pathMatchers("/management/health/**").permitAll()
                        .pathMatchers("/management/info").permitAll()
                        .pathMatchers("/management/prometheus").permitAll()
                        .pathMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN)
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "https://localhost:4200", "http://localhost:4200",
                "https://localhost:4300", "http://localhost:4300"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setExposedHeaders(List.of("Authorization", "X-Total-Count"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/management/**", config);
        source.registerCorsConfiguration("/services/**", config);
        source.registerCorsConfiguration("/payment/api/**", config);
        return source;
    }
}
