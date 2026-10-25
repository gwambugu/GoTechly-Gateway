package com.dotechly.gotechly.gateway.service;

import com.dotechly.gotechly.gateway.config.AdProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
/**
 * Read-only directory lookups performed with the service account.
 * Blocking JNDI/LDAP calls are pushed onto {@link Schedulers#boundedElastic()} so they
 * never run on a Netty event-loop thread.
 */
@Service
@ConditionalOnProperty(prefix = "gotechly.security.ad", name = "enabled", havingValue = "true")
public class AdDirectoryService {

    private static final Logger log = LoggerFactory.getLogger(AdDirectoryService.class);

    /** Bit 0x0002 of userAccountControl — ACCOUNTDISABLE. */
    private static final int ADS_UF_ACCOUNTDISABLE = 0x0002;

    private final LdapTemplate ldapTemplate;
    private final AdProperties ad;

    public AdDirectoryService(LdapTemplate ldapTemplate, AdProperties ad) {
        this.ldapTemplate = ldapTemplate;
        this.ad = ad;
    }

    /**
     * Canonical login form: a lowercase UPN.
     *
     * <p>A bare directory name ({@code geoffrey.kamau}) gets the configured domain appended so the
     * registration existence search, the availability check and the AD bind all agree on one key —
     * {@code userPrincipalName} is always {@code user@domain}, so a bare name never matches
     * {@link #userExists}. The break-glass local admin is exempt: it is a local account with no UPN.
     */
    public String toUpn(String login) {
        if (login == null) {
            return null;
        }
        String normalized = login.trim().toLowerCase();
        if (normalized.isEmpty() || normalized.indexOf('@') >= 0) {
            return normalized;
        }
        if (normalized.equals(ad.getLocalAdminLogin() == null ? null : ad.getLocalAdminLogin().toLowerCase())) {
            return normalized;
        }
        String domain = ad.getDomain();
        return (domain == null || domain.isBlank()) ? normalized : normalized + "@" + domain.trim().toLowerCase();
    }

    /** The only attributes needed to derive a bind principal; requested explicitly. */
    private static final String[] IDENTITY_ATTRIBUTES = { "userPrincipalName", "sAMAccountName" };

    /**
     * Resolves what the caller typed to a principal that can actually bind.
     *
     * <p>Returns empty when no entry matches. Used at registration time, where the resolved value —
     * not the string the user typed — becomes the stored login: the search filter may match on
     * {@code mail}, and a mail address is frequently in a different domain than the UPN
     * ({@code jane.doe@corp.example.com} vs {@code jane.doe@example.com}). Storing
     * the typed value let registration succeed and then made the login bind impossible, because
     * {@link #bindPrincipal} appends the UPN suffix to anything that does not already carry it.
     *
     * <p>Preference order: {@code userPrincipalName}, then {@code sAMAccountName@<domain>} — AD
     * accepts the latter as an <em>implicit UPN</em> even for accounts with no explicit one. If the
     * entry exposes neither, this <b>fails</b> rather than falling back to what was typed: a
     * silently-stored unbindable login is what produced accounts that could register and then never
     * sign in. Both attributes are requested explicitly, so an entry that returns neither means the
     * service account cannot read them, or the filter matched a contact/group rather than a user.
     */
    public Mono<Optional<String>> findUpn(String login) {
        return Mono.fromCallable(() -> {
            String filter = ad.getUserSearchFilter().replace("{0}", LdapEncoder.filterEncode(login));
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            controls.setReturningAttributes(IDENTITY_ATTRIBUTES);

            List<String> principals = ldapTemplate.search("", filter, controls,
                    (AttributesMapper<String>) attrs -> {
                        String upn = value(attrs, "userPrincipalName");
                        if (upn != null) {
                            return upn;
                        }
                        String sam = value(attrs, "sAMAccountName");
                        String domain = ad.getDomain();
                        if (sam != null && domain != null && !domain.isBlank()) {
                            log.info("Directory entry matching {} has no userPrincipalName; using the implicit UPN "
                                    + "{}@{}", login, sam, domain.trim());
                            return sam + "@" + domain.trim();
                        }
                        return null;
                    });

            if (principals.isEmpty()) {
                return Optional.<String>empty();
            }
            String principal = principals.stream().filter(Objects::nonNull).findFirst().orElse(null);
            if (principal == null) {
                throw new IllegalStateException("Directory entry matching '" + login + "' exposes neither "
                        + "userPrincipalName nor sAMAccountName, so no bind principal can be derived. Check that the "
                        + "service account may read those attributes and that the search filter is not matching a "
                        + "contact or group object.");
            }
            return Optional.of(principal.trim().toLowerCase());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private static String value(Attributes attrs, String name) throws NamingException {
        Attribute a = attrs.get(name);
        Object v = (a == null) ? null : a.get();
        String s = (v == null) ? null : v.toString().trim();
        return (s == null || s.isEmpty()) ? null : s;
    }

    /**
     * The principal {@code ActiveDirectoryLdapAuthenticationProvider} will actually bind with —
     * mirrors its private {@code createBindPrincipal}. Logging only: it is the one value that
     * distinguishes a mistyped password from a login that can never bind.
     */
    public String bindPrincipal(String login) {
        String domain = ad.getDomain();
        if (login == null || domain == null || domain.isBlank()
                || login.toLowerCase().endsWith(domain.trim().toLowerCase())) {
            return login;
        }
        return login + "@" + domain.trim();
    }

    /**
     * Deterministic disabled-check via the {@code userAccountControl} attribute — independent of
     * how a failed bind reports itself.
     *
     * @return {@code Optional.of(true)} disabled, {@code Optional.of(false)} enabled,
     *         {@code Optional.empty()} not found / attribute absent.
     */
    public Mono<Optional<Boolean>> isDisabledInDirectory(String upn) {
        return Mono.fromCallable(() -> {
            String filter = ad.getUserSearchFilter().replace("{0}", LdapEncoder.filterEncode(upn));
            List<Integer> uac = ldapTemplate.search("", filter, (AttributesMapper<Integer>) attrs -> {
                Attribute a = attrs.get("userAccountControl");
                return a == null ? null : Integer.valueOf(a.get().toString());
            });
            if (uac.isEmpty() || uac.get(0) == null) {
                return Optional.<Boolean>empty();
            }
            return Optional.of((uac.get(0) & ADS_UF_ACCOUNTDISABLE) != 0);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
