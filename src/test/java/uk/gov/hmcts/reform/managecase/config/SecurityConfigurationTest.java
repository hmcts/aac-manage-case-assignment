package uk.gov.hmcts.reform.managecase.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.security.JwtGrantedAuthoritiesConverter;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigurationTest.ProtectedController.class)
@AutoConfigureMockMvc
@Import({
    SecurityConfiguration.class,
    JwtGrantedAuthoritiesConverter.class,
    SecurityConfigurationTest.ProtectedController.class,
    SecurityConfigurationTest.TestSecurityConfiguration.class
})
@TestPropertySource(properties = {
    "spring.security.oauth2.client.provider.oidc.issuer-uri=http://idam.example/o",
    "oidc.issuer=http://idam.example/o"
})
class SecurityConfigurationTest {

    private static final String OIDC_CLIENT_ID_PROPERTY = "spring.security.oauth2.client.registration.oidc.client-id";
    private static final String OIDC_CLIENT_SECRET_PROPERTY =
        "spring.security.oauth2.client.registration.oidc.client-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private IdamRepository idamRepository;

    @BeforeEach
    void setUp() {
        when(jwtDecoder.decode(anyString())).thenReturn(jwt());
    }

    @Test
    void shouldAuthenticateBearerJwtWithoutOauth2ClientRegistration() throws Exception {
        assertThat(applicationContext.getEnvironment().getProperty(OIDC_CLIENT_ID_PROPERTY)).isNull();
        assertThat(applicationContext.getEnvironment().getProperty(OIDC_CLIENT_SECRET_PROPERTY)).isNull();

        mockMvc.perform(get("/test-secured")
                .header("Authorization", "Bearer user-token")
                .header(ServiceAuthFilter.AUTHORISATION, "s2s-token"))
            .andExpect(status().isOk())
            .andExpect(content().string("user-id"));
    }

    private Jwt jwt() {
        Instant now = Instant.now();

        return Jwt.withTokenValue("user-token")
            .header("alg", "none")
            .claim("sub", "user-id")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300))
            .build();
    }

    @RestController
    public static class ProtectedController {

        @GetMapping("/test-secured")
        public String secured(Authentication authentication) {
            return authentication.getName();
        }
    }

    @TestConfiguration
    static class TestSecurityConfiguration {

        @Bean
        ServiceAuthFilter serviceAuthFilter() {
            return new ServiceAuthFilter(authTokenValidator(), List.of("xui_webapp"));
        }

        @Bean
        AuthTokenValidator authTokenValidator() {
            return new AuthTokenValidator() {
                @Override
                public void validate(String token) {
                    // Test-only validator accepts the service token so the JWT resource-server path is exercised.
                }

                @Override
                public void validate(String token, List<String> roles) {
                    // Test-only validator accepts the service token so the JWT resource-server path is exercised.
                }

                @Override
                public String getServiceName(String token) {
                    return "xui_webapp";
                }
            };
        }
    }
}
