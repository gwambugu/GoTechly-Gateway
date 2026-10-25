package com.dotechly.gotechly.gateway.security;

/**
 * Constants for Spring Security authorities.
 */
public final class AuthoritiesConstants {

    public static final String ADMIN = "ROLE_ADMIN";

    public static final String USER = "ROLE_USER";

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";

    public static final String PARTNER_MAKER = "ROLE_PARTNER_MAKER";

    public static final String PARTNER_CHECKER = "ROLE_PARTNER_CHECKER";

    public static final String PARTNER_VIEWER = "ROLE_PARTNER_VIEWER";
    /**
     * Carried by tokens minted from an API key/secret at {@code POST /api/auth/token}. Satisfies the
     * {@code .authenticated()} rule on the payment APIs and nothing more — never grant a machine
     * credential ADMIN or PARTNER_* by adding it to those rules.
     */
    public static final String API_CLIENT = "ROLE_API_CLIENT";
    private AuthoritiesConstants() {}
}
