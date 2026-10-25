package com.dotechly.gotechly.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "gotechly.gateway")
public class GatewayServicesProperties {

    private List<ServiceConfig> services = new ArrayList<>();

    public List<ServiceConfig> getServices() { return services; }
    public void setServices(List<ServiceConfig> services) { this.services = services; }

    public static class ServiceConfig {
        private String name;
        private String baseUrl;
        private String healthPath = "/actuator/health";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getHealthPath() { return healthPath; }
        public void setHealthPath(String healthPath) { this.healthPath = healthPath; }
    }
}
