package com.dotechly.gotechly.gateway;




import com.dotechly.gotechly.gateway.config.AppConstants;
import com.dotechly.gotechly.gateway.config.CRLFLogConverter;
import com.dotechly.gotechly.gateway.config.MultiPortProperties;
import com.dotechly.gotechly.gateway.config.GoTechlyCoreLbConfig;
import com.dotechly.gotechly.gateway.util.DefaultProfileUtil;
import jakarta.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;


@SpringBootApplication
@LoadBalancerClient(name = "gotechly-core", configuration = GoTechlyCoreLbConfig.class)
public class GoTechlyGatewayApplication {
	private static final Logger LOG = LoggerFactory.getLogger(GoTechlyGatewayApplication.class);
	@Autowired
	private Environment env;

	public static ConfigurableApplicationContext applicationContext;

	@PostConstruct
	public void initApplication() {
		Collection<String> activeProfiles = Arrays.asList(env.getActiveProfiles());
		if (
				activeProfiles.contains(AppConstants.SPRING_PROFILE_DEVELOPMENT) &&
						activeProfiles.contains(AppConstants.SPRING_PROFILE_PRODUCTION)
		) {
			LOG.error(
					"You have misconfigured your application! It should not run " + "with both the 'dev' and 'prod' profiles at the same time."
			);
		}
		if (
				activeProfiles.contains(AppConstants.SPRING_PROFILE_DEVELOPMENT) &&
						activeProfiles.contains(AppConstants.SPRING_PROFILE_CLOUD)
		) {
			LOG.error(
					"You have misconfigured your application! It should not " + "run with both the 'dev' and 'cloud' profiles at the same time."
			);
		}
	}

	public static void main(String[] args) {
		//System.setProperty("pf4j.mode", "development");
		SpringApplication app = new SpringApplication(GoTechlyGatewayApplication.class);
		DefaultProfileUtil.addDefaultProfile(app);
		applicationContext = app.run(args);
		logApplicationStartup(applicationContext.getEnvironment());
	}

	private static void logApplicationStartup(Environment env) {
		String protocol = Optional.ofNullable(env.getProperty("server.ssl.key-store")).map(key -> "https").orElse("http");
		String applicationName = env.getProperty("spring.application.name");
		String serverPort = env.getProperty("server.port");
		String contextPath = Optional.ofNullable(env.getProperty("server.servlet.context-path"))
				.filter(StringUtils::isNotBlank)
				.orElse("/");
		String hostAddress = "localhost";
		try {
			hostAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			LOG.warn("The host name could not be determined, using `localhost` as fallback");
		}

		// Build webapp port info
		MultiPortProperties multiPortProps = applicationContext.getBean(MultiPortProperties.class);
		MultiPortProperties.WebappConfig webappConfig = multiPortProps.getWebapp();
		String webappSection = "";
		if (webappConfig != null && webappConfig.getPort() > 0) {
			boolean webappTls = webappConfig.getTls() != null && webappConfig.getTls().isEnabled();
			String webappProtocol = webappTls ? "https" : "http";
			webappSection = "\n\tWebapp UI: \t" + webappProtocol + "://localhost:" + webappConfig.getPort() + "/";
		}

		MultiPortProperties.WebappPortalConfig partnerConfig = multiPortProps.getWebappPartner();
		String partnerSection = "";
		if (partnerConfig != null && partnerConfig.getPort() > 0) {
			boolean partnerTls = partnerConfig.getTls() != null && partnerConfig.getTls().isEnabled();
			String partnerProtocol = partnerTls ? "https" : "http";
			partnerSection = "\n\tPartner Portal: \t" + partnerProtocol + "://localhost:" + partnerConfig.getPort() + "/";
		}

		// Build additional ports info
		StringBuilder additionalPorts = new StringBuilder();
		for (var entry : multiPortProps.getPorts().entrySet()) {
			String name = entry.getKey();
			MultiPortProperties.PortConfig config = entry.getValue();
			boolean tls = config.getTls() != null && config.getTls().isEnabled();
			String portProtocol = tls ? "https" : "http";
			String paths = String.join(", ", config.getAllowedPaths());
			additionalPorts.append(String.format(
					"\n\t\t%-16s %s://localhost:%-6d paths: %s",
					name, portProtocol, config.getPort(), paths));
		}

		String portsSection = additionalPorts.isEmpty()
				? ""
				: "\n\tAdditional Ports:" + additionalPorts;

		LOG.info(
				CRLFLogConverter.CRLF_SAFE_MARKER,
				"""

                        ----------------------------------------------------------
                        \tApplication '{}' is running! Access URLs:
                        \tLocal: \t\t{}://localhost:{}{}
                        \tExternal: \t{}://{}:{}{}
                        \tProfile(s): \t{}{}{}{}
                        ----------------------------------------------------------""",
				applicationName,
				protocol,
				serverPort,
				contextPath,
				protocol,
				hostAddress,
				serverPort,
				contextPath,
				env.getActiveProfiles().length == 0 ? env.getDefaultProfiles() : env.getActiveProfiles(),
				webappSection,
				partnerSection,
				portsSection
		);
	}

}