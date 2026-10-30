package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.managecase.Application;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.caseDetails;
import static uk.gov.hmcts.reform.managecase.TestFixtures.IdamFixture.userDetails;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ORGANIZATION_ID;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.user;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.usersByOrganisation;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient.CASE_USERS;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubAssignCase;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseDetailsByCaseIdViaExternalApi;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetUsersByOrganisationExternal;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubIdamGetUserById;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

@ActiveProfiles("integration")
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0, stubs = "classpath:/wiremock-stubs")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CaseAssignmentRealHttpIT {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String ASSIGNEE_ID = "ae2eb34c-816a-4eea-b714-6654d022fcef";
    private static final String CASE_ID = "1588234985453946";
    private static final String ORG_POLICY_ROLE = "[CaseRole1]";

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("user-token")
            .header("alg", "none")
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(3600))
            .subject("445")
            .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);

        stubGetUsersByOrganisationExternal(usersByOrganisation(user(ASSIGNEE_ID)));
        stubIdamGetUserById(ASSIGNEE_ID, userDetails(ASSIGNEE_ID,
            "caseworker-AUTOTEST1-solicitor", "caseworker-AUTOTEST1"));
        stubGetCaseDetailsByCaseIdViaExternalApi(CASE_ID, caseDetails(ORGANIZATION_ID, ORG_POLICY_ROLE));
        stubAssignCase(CASE_ID, ASSIGNEE_ID, ORG_POLICY_ROLE);
    }

    @Test
    void shouldReturnCompleteCaseAssignmentResponseOverRealHttp() throws Exception {
        CaseAssignmentRequest payload = new CaseAssignmentRequest(CASE_TYPE_ID, CASE_ID, ASSIGNEE_ID);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/case-assignments?use_user_token=true"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
            .header(SERVICE_AUTHORIZATION, "Bearer service-token")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
            .build();

        HttpResponse<byte[]> response = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build()
            .send(request, HttpResponse.BodyHandlers.ofByteArray());
        String responseBody = new String(response.body(), StandardCharsets.UTF_8);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.headers().firstValue(HttpHeaders.CONTENT_ENCODING)).isEmpty();
        assertThat(objectMapper.readTree(responseBody).path("status_message").asText())
            .isEqualTo("Roles [CaseRole1] from the organisation policies successfully assigned to the assignee.");
        verify(postRequestedFor(urlEqualTo(CASE_USERS)));
    }
}
