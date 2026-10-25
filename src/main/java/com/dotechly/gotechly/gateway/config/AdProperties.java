package com.dotechly.gotechly.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Active Directory authentication settings.
 * Bound from {@code gotechly.security.ad.*}.
 */
@ConfigurationProperties(prefix = "gotechly.security.ad")
public class AdProperties {

    /** When false, the gateway falls back to local BCrypt authentication (dev/no-AD). */
    private boolean enabled;

    /** LDAPS URL of a domain controller, e.g. {@code ldaps://dc01.corp.example.com:636}. */
    private String url;

    /** UPN suffix / AD domain, e.g. {@code corp.example.com}. */
    private String domain;

    /** Search base DN, e.g. {@code DC=corp,DC=example,DC=com}. */
    private String baseDn;

    /** Service account (UPN form) used for the registration existence search. */
    private String serviceAccountUpn;

    /** Service account password — supply via environment/secret, never commit. */
    private String serviceAccountPassword;

    /** Filter for locating a user; {@code {0}} is replaced with the (escaped) UPN. */
    private String userSearchFilter = "(userPrincipalName={0})";

    /** The single break-glass account that authenticates locally (auth_source = LOCAL). */
    private String localAdminLogin = "admin";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getServiceAccountUpn() {
        return serviceAccountUpn;
    }

    public void setServiceAccountUpn(String serviceAccountUpn) {
        this.serviceAccountUpn = serviceAccountUpn;
    }

    public String getServiceAccountPassword() {
        return serviceAccountPassword;
    }

    public void setServiceAccountPassword(String serviceAccountPassword) {
        this.serviceAccountPassword = serviceAccountPassword;
    }

    public String getUserSearchFilter() {
        return userSearchFilter;
    }

    public void setUserSearchFilter(String userSearchFilter) {
        this.userSearchFilter = userSearchFilter;
    }

    public String getLocalAdminLogin() {
        return localAdminLogin;
    }

    public void setLocalAdminLogin(String localAdminLogin) {
        this.localAdminLogin = localAdminLogin;
    }
}
