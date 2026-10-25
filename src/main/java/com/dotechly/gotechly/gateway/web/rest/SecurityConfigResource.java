package com.dotechly.gotechly.gateway.web.rest;

import com.dotechly.gotechly.gateway.config.AdProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Public, pre-authentication view of the auth mode so the SPA can shape its own forms —
 * chiefly: under AD the register page must not ask for a password (the directory owns the
 * credential; {@code UserService.registerUser} discards anything sent), and it can hint the
 * UPN suffix on the username field.
 *
 * <p>Deliberately narrow: the enabled flag and the UPN suffix only. No URL, base DN or service
 * account — this endpoint is unauthenticated.
 */
@RestController
@RequestMapping("/api/security-config")
public class SecurityConfigResource {

    /** Present only when {@code gotechly.security.ad.enabled=true} (see {@code LdapConfiguration}). */
    private final ObjectProvider<AdProperties> adPropertiesProvider;

    public SecurityConfigResource(ObjectProvider<AdProperties> adPropertiesProvider) {
        this.adPropertiesProvider = adPropertiesProvider;
    }

    @GetMapping
    public Mono<Map<String, Object>> get() {
        AdProperties ad = adPropertiesProvider.getIfAvailable();
        Map<String, Object> body = new HashMap<>();
        body.put("adEnabled", ad != null && ad.isEnabled());
        body.put("domain", ad == null ? null : ad.getDomain());
        return Mono.just(body);
    }
}
