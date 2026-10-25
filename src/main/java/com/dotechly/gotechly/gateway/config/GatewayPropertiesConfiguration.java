package com.dotechly.gotechly.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ GatewayServicesProperties.class, MultiPortProperties.class })
public class GatewayPropertiesConfiguration {
}
