package uk.gov.hmcts.reform.managecase;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.context.ContextCleanupListener;

@Configuration
public class TestIdamConfiguration extends ContextCleanupListener {

    @Bean
    // Overriding as OAuth2ClientRegistrationRepositoryConfiguration loading before
    // wire-mock mappings for /o/.well-known/openid-configuration
    public ReactiveClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryReactiveClientRegistrationRepository(clientRegistration());
    }

    private ClientRegistration clientRegistration() {
        return ClientRegistration.withRegistrationId("oidc")
            .redirectUri("{baseUrl}/{action}/oauth2/code/{registrationId}")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .scope("read:user")
            .authorizationUri("http://idam/o/authorize")
            .tokenUri("http://idam/o/access_token")
            .userInfoUri("http://idam/o/userinfo")
            .userNameAttributeName("id")
            .clientName("Client Name")
            .clientId("client-id")
            .clientSecret("client-secret")
            .build();
    }
}
