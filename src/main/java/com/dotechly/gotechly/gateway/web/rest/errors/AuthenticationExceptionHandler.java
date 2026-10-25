package com.dotechly.gotechly.gateway.web.rest.errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Turns authentication failures into proper 401/403 responses.
 *
 * <p>Spring Security's {@code ExceptionTranslationWebFilter} only translates
 * {@link org.springframework.security.access.AccessDeniedException}; an
 * {@link AuthenticationException} raised inside a controller — which is exactly what
 * {@code UserJWTController.authorize} produces when the authentication manager rejects a login —
 * is not intercepted by anything and reaches the default error handler as a <b>500</b>. That made
 * a wrong password, an account still pending approval and a genuine server fault indistinguishable
 * to the caller.
 *
 * <p>The body carries a stable {@code error} key plus a human-readable {@code detail} so the SPA
 * can tell a user awaiting approval to wait, rather than telling them their password is wrong.
 */
@RestControllerAdvice
public class AuthenticationExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handle(AuthenticationException ex) {
        return switch (ex) {
            case DisabledException e -> body(HttpStatus.FORBIDDEN, "account_disabled", e.getMessage());
            case LockedException e -> body(HttpStatus.FORBIDDEN, "account_locked",
                "Account is locked in the directory. Contact IT support.");
            case CredentialsExpiredException e -> body(HttpStatus.FORBIDDEN, "credentials_expired",
                "Your directory password has expired. Change it, then sign in again.");
            case AccountExpiredException e -> body(HttpStatus.FORBIDDEN, "account_expired",
                "Your directory account has expired. Contact IT support.");
            // Everything else is deliberately opaque: never reveal whether the account exists.
            default -> body(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid username or password");
        };
    }

    private ResponseEntity<Map<String, String>> body(HttpStatus status, String error, String detail) {
        return ResponseEntity.status(status).body(Map.of("error", error, "detail", detail));
    }
}
