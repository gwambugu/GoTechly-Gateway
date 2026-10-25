package com.dotechly.gotechly.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;

/**
 * Active Directory wiring. Only active when {@code gotechly.security.ad.enabled=true};
 * otherwise the gateway authenticates locally (see {@code SecurityConfiguration}).
 *
 * <ul>
 *   <li>{@link LdapContextSource}/{@link LdapTemplate} — service-account bind used by
 *       {@code AdDirectoryService} for the registration existence search and the
 *       {@code userAccountControl} disabled-check.</li>
 *   <li>{@link ActiveDirectoryLdapAuthenticationProvider} — binds AS the user at login.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(AdProperties.class)
@ConditionalOnProperty(prefix = "gotechly.security.ad", name = "enabled", havingValue = "true")
public class LdapConfiguration {

    @Bean
    public LdapContextSource ldapContextSource(AdProperties ad) {
        LdapContextSource ctx = new LdapContextSource();
        ctx.setUrl(ad.getUrl());                       // ldaps://...:636
        ctx.setBase(ad.getBaseDn());
        ctx.setUserDn(ad.getServiceAccountUpn());
        ctx.setPassword(ad.getServiceAccountPassword());
        ctx.setPooled(true);
        return ctx;
    }

    @Bean
    public LdapTemplate ldapTemplate(LdapContextSource ldapContextSource) {
        LdapTemplate template = new LdapTemplate(ldapContextSource);
        // AD emits continuation references for anything outside the searched partition; with the
        // JNDI default (referral=ignore) those surface as PartialResultException at the end of an
        // otherwise successful enumeration. Standard practice against AD is to swallow them.
        template.setIgnorePartialResultException(true);
        return template;
    }

    @Bean
    public ActiveDirectoryLdapAuthenticationProvider adAuthenticationProvider(AdProperties ad) {
        // Domain is deliberately null. Its ONLY effect in this provider is createBindPrincipal(),
        // which appends "@{domain}" to any username not already ending in it — and every login we
        // pass is the directory's own userPrincipalName, resolved by AdDirectoryService.findUpn.
        // A forest with an alternative UPN suffix (users at @corp.example.com inside
        // example.com) would otherwise be rewritten to user@corp.example.com@example.com,
        // which binds as AD sub-error 52e and is indistinguishable from a wrong password.
        // Suffix handling for bare names stays in AdDirectoryService.toUpn, where one property
        // governs it. rootDn is supplied explicitly, so nothing else here needs the domain.
        ActiveDirectoryLdapAuthenticationProvider provider =
                new ActiveDirectoryLdapAuthenticationProvider(null, ad.getUrl(), ad.getBaseDn());
        // Map AD sub-error codes (533 disabled, 532 password expired, 701 account expired,
        // 775 locked) to typed exceptions — used as a fast-path hint; userAccountControl
        // remains the source of truth in AdDirectoryService.
        provider.setConvertSubErrorCodesToExceptions(true);
        // We issue our own JWT from app-owned roles, so AD group authorities are irrelevant.
        provider.setUseAuthenticationRequestCredentials(true);
        return provider;
    }
}
