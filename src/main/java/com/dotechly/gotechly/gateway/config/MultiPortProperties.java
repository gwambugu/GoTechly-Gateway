package com.dotechly.gotechly.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "gotechly.gateway.multi-port")
public class MultiPortProperties {

    private Map<String, PortConfig> ports = new LinkedHashMap<>();
    private WebappConfig webapp;
    private WebappPortalConfig webappPartner;

    public Map<String, PortConfig> getPorts() { return ports; }
    public void setPorts(Map<String, PortConfig> ports) { this.ports = ports; }

    public WebappConfig getWebapp() { return webapp; }
    public void setWebapp(WebappConfig webapp) { this.webapp = webapp; }

    public WebappPortalConfig getWebappPartner() { return webappPartner; }
    public void setWebappPartner(WebappPortalConfig webappPartner) { this.webappPartner = webappPartner; }

    public int getWebappPort() {
        return webapp != null ? webapp.getPort() : 0;
    }

    public int getWebappPartnerPort() {
        return webappPartner != null ? webappPartner.getPort() : 0;
    }

    public static class WebappConfig {
        private int port = 0;
        private TlsConfig tls;

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public TlsConfig getTls() { return tls; }
        public void setTls(TlsConfig tls) { this.tls = tls; }
    }

    public static class WebappPortalConfig {
        private int port = 0;
        private List<String> allowedPaths = new ArrayList<>();
        private TlsConfig tls;

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public List<String> getAllowedPaths() { return allowedPaths; }
        public void setAllowedPaths(List<String> allowedPaths) { this.allowedPaths = allowedPaths; }
        public TlsConfig getTls() { return tls; }
        public void setTls(TlsConfig tls) { this.tls = tls; }
    }

    public static class PortConfig {
        private int port;
        private List<String> allowedPaths = new ArrayList<>();
        private List<String> allowedIps = new ArrayList<>();
        private List<String> trustedProxies = new ArrayList<>();
        private TlsConfig tls;

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public List<String> getAllowedPaths() { return allowedPaths; }
        public void setAllowedPaths(List<String> allowedPaths) { this.allowedPaths = allowedPaths; }
        public List<String> getAllowedIps() { return allowedIps; }
        public void setAllowedIps(List<String> allowedIps) { this.allowedIps = allowedIps; }
        public List<String> getTrustedProxies() { return trustedProxies; }
        public void setTrustedProxies(List<String> trustedProxies) { this.trustedProxies = trustedProxies; }
        public TlsConfig getTls() { return tls; }
        public void setTls(TlsConfig tls) { this.tls = tls; }
    }

    public static class TlsConfig {
        private boolean enabled = false;
        private boolean useServerDefault = false;
        private String keyStore;
        private String keyStorePassword;
        private String keyStoreType = "JKS";
        private String keyAlias;
        private String trustStore;
        private String trustStorePassword;
        private String trustStoreType = "JKS";
        private ClientAuth clientAuth = ClientAuth.NONE;
        private List<String> protocols = List.of("TLSv1.2", "TLSv1.3");
        private List<String> ciphers;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isUseServerDefault() { return useServerDefault; }
        public void setUseServerDefault(boolean useServerDefault) { this.useServerDefault = useServerDefault; }
        public String getKeyStore() { return keyStore; }
        public void setKeyStore(String keyStore) { this.keyStore = keyStore; }
        public String getKeyStorePassword() { return keyStorePassword; }
        public void setKeyStorePassword(String keyStorePassword) { this.keyStorePassword = keyStorePassword; }
        public String getKeyStoreType() { return keyStoreType; }
        public void setKeyStoreType(String keyStoreType) { this.keyStoreType = keyStoreType; }
        public String getKeyAlias() { return keyAlias; }
        public void setKeyAlias(String keyAlias) { this.keyAlias = keyAlias; }
        public String getTrustStore() { return trustStore; }
        public void setTrustStore(String trustStore) { this.trustStore = trustStore; }
        public String getTrustStorePassword() { return trustStorePassword; }
        public void setTrustStorePassword(String trustStorePassword) { this.trustStorePassword = trustStorePassword; }
        public String getTrustStoreType() { return trustStoreType; }
        public void setTrustStoreType(String trustStoreType) { this.trustStoreType = trustStoreType; }
        public ClientAuth getClientAuth() { return clientAuth; }
        public void setClientAuth(ClientAuth clientAuth) { this.clientAuth = clientAuth; }
        public List<String> getProtocols() { return protocols; }
        public void setProtocols(List<String> protocols) { this.protocols = protocols; }
        public List<String> getCiphers() { return ciphers; }
        public void setCiphers(List<String> ciphers) { this.ciphers = ciphers; }
    }

    public enum ClientAuth {
        NONE, WANT, NEED
    }
}
