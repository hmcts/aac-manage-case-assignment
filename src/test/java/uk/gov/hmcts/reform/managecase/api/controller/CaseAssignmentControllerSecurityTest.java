package uk.gov.hmcts.reform.managecase.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.managecase.config.MapperConfig;
import uk.gov.hmcts.reform.managecase.config.SecurityConfiguration;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CaseAssignmentController.class)
@AutoConfigureMockMvc
@Import({
    MapperConfig.class,
    SecurityConfiguration.class,
    JwtGrantedAuthoritiesConverter.class,
    CaseAssignmentControllerSecurityTest.TestSecurityConfiguration.class
})
@TestPropertySource(properties = {
    "spring.security.oauth2.client.provider.oidc.issuer-uri=http://idam.example/o",
    "oidc.issuer=http://idam.example/o"
})
class CaseAssignmentControllerSecurityTest {

    @MockitoBean
    private CaseAssignmentService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private IdamRepository idamRepository;

    @MockitoBean
    private WebEndpointsSupplier webEndpointsSupplier;

    @MockitoBean
    private WebMvcEndpointHandlerMapping webMvcEndpointHandlerMapping;

    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(jwtDecoder.decode(anyString())).thenReturn(jwt());
        when(service.getCaseAssignments(List.of("1588234985453946"))).thenReturn(List.of());
    }

    @Test
    void rejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get(CaseAssignmentController.CASE_ASSIGNMENTS_PATH)
                .param("case_ids", "1588234985453946"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsRequestWithValidUserAndServiceTokens() throws Exception {
        mockMvc.perform(get(CaseAssignmentController.CASE_ASSIGNMENTS_PATH)
                .param("case_ids", "1588234985453946")
                .header("Authorization", "Bearer user-token")
                .header(ServiceAuthFilter.AUTHORISATION, "s2s-token"))
            .andExpect(status().isOk());
    }

    @Test
    void rejectsRequestWithValidUserButMissingServiceToken() throws Exception {
        mockMvc.perform(get(CaseAssignmentController.CASE_ASSIGNMENTS_PATH)
                .param("case_ids", "1588234985453946")
                .header("Authorization", "Bearer user-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRequestWithInvalidServiceToken() throws Exception {
        mockMvc.perform(get(CaseAssignmentController.CASE_ASSIGNMENTS_PATH)
                .param("case_ids", "1588234985453946")
                .header("Authorization", "Bearer user-token")
                .header(ServiceAuthFilter.AUTHORISATION, "Bearer rejected-s2s-token"))
            .andExpect(status().isForbidden());
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

    @TestConfiguration
    static class TestSecurityConfiguration {

        @Bean
        ServiceAuthFilter serviceAuthFilter(AuthTokenValidator authTokenValidator) {
            return new ServiceAuthFilter(authTokenValidator, List.of("xui_webapp"));
        }

        @Bean
        AuthTokenValidator authTokenValidator() {
            return new AuthTokenValidator() {
                @Override
                public void validate(String token) {
                    rejectInvalidToken(token);
                }

                @Override
                public void validate(String token, List<String> roles) {
                    rejectInvalidToken(token);
                }

                @Override
                public String getServiceName(String token) {
                    return token.contains("rejected-s2s-token") ? "invalid-service" : "xui_webapp";
                }

                private void rejectInvalidToken(String token) {
                    if (token.contains("rejected-s2s-token")) {
                        throw new InvalidTokenException("invalid service token");
                    }
                }
            };
        }
    }
}
