package com.dotechly.gotechly.gateway.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class TokenProvider {

    private final Logger log = LoggerFactory.getLogger(TokenProvider.class);

    private static final String AUTHORITIES_KEY = "auth";

    private final Key key;

    private final JwtParser jwtParser;

    private final long tokenValidityInMilliseconds;

    private final long tokenValidityInMillisecondsForRememberMe;

    public TokenProvider(
        @Value("${jhipster.security.authentication.jwt.base64-secret:YmFzZTY0LXNlY3JldC1rZXktZm9yLXhhY3Rwb2ludC1nYXRld2F5LW11c3QtYmUtYXQtbGVhc3QtNTEyLWJpdHM=}") String base64Secret,
        @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds:3600}") long tokenValidityInSeconds,
        @Value("${jhipster.security.authentication.jwt.token-validity-in-seconds-for-remember-me:2592000}") long tokenValidityInSecondsForRememberMe
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.jwtParser = Jwts.parser().setSigningKey(key).build();
        this.tokenValidityInMilliseconds = 1000 * tokenValidityInSeconds;
        this.tokenValidityInMillisecondsForRememberMe = 1000 * tokenValidityInSecondsForRememberMe;
    }

    public String createToken(Authentication authentication, boolean rememberMe) {
        return createToken(authentication, rememberMe
            ? this.tokenValidityInMillisecondsForRememberMe
            : this.tokenValidityInMilliseconds);
    }

    /**
     * Mint a token with an explicit lifetime. Used for machine tokens, which are deliberately
     * shorter-lived than a human session: an API client re-authenticates cheaply and silently, so
     * the window in which a leaked token is useful should be small.
     */
    public String createToken(Authentication authentication, Duration validity) {
        return createToken(authentication, validity.toMillis());
    }

    private String createToken(Authentication authentication, long validityInMilliseconds) {
        String authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));

        Date expiry = new Date((new Date()).getTime() + validityInMilliseconds);

        return Jwts.builder()
            .setSubject(authentication.getName())
            .claim(AUTHORITIES_KEY, authorities)
            .signWith(key, SignatureAlgorithm.HS512)
            .setExpiration(expiry)
            .compact();
    }

    /** Lifetime of a standard (non remember-me) token, for {@code expires_in} on the token endpoint. */
    public long getTokenValidityInSeconds() {
        return tokenValidityInMilliseconds / 1000;
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaimsOrNull(token);
        if (claims == null) {
            throw new IllegalArgumentException("Invalid JWT token — cannot extract authentication");
        }

        Collection<? extends GrantedAuthority> authorities = Arrays
            .stream(claims.get(AUTHORITIES_KEY).toString().split(","))
            .filter(auth -> !auth.trim().isEmpty())
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        User principal = new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public boolean validateToken(String authToken) {
        return parseClaimsOrNull(authToken) != null;
    }

    private Claims parseClaimsOrNull(String token) {
        try {
            return jwtParser.parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            log.trace("Expired JWT token.");
        } catch (UnsupportedJwtException e) {
            log.trace("Unsupported JWT token.");
        } catch (MalformedJwtException e) {
            log.trace("Malformed JWT token.");
        } catch (SignatureException e) {
            log.trace("Invalid JWT signature.");
        } catch (IllegalArgumentException e) {
            log.error("Token validation error {}", e.getMessage());
        }
        return null;
    }
}
