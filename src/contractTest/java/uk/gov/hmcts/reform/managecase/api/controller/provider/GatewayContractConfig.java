package uk.gov.hmcts.reform.managecase.api.controller.provider;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import uk.gov.hmcts.reform.managecase.api.errorhandling.RestExceptionHandler;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/** Loads production gateway routes/filters, with a controlled downstream only. */
@Configuration
@EnableWebMvc
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class,
    OAuth2ClientAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class
})
@Import(RestExceptionHandler.class)
public class GatewayContractConfig {

    private static final WireMockServer DOWNSTREAM = new WireMockServer(options().dynamicPort());

    public static synchronized String downstreamUrl() {
        if (!DOWNSTREAM.isRunning()) {
            DOWNSTREAM.start();
        }
        return DOWNSTREAM.baseUrl();
    }

    @Bean(destroyMethod = "stop")
    public WireMockServer gatewayDownstream() {
        downstreamUrl();
        return DOWNSTREAM;
    }
}
