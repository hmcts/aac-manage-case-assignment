package uk.gov.hmcts.reform.managecase.api.controller;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.managecase.BaseIT;
import uk.gov.hmcts.reform.managecase.TestFixtures;

import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.caseDetails;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubSearchCase;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubSearchCaseWithPrefix;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.MethodNamingConventions", "PMD.AvoidDuplicateLiterals"})
public class SpringCloudGatewayDataStoreRequestIT extends BaseIT {

    private static final String CASE_TYPE_ID = "CT_MasterCase";
    private static final String PATH = "/ccd/searchCases?ctid=CT_MasterCase";
    private static final String PATH_INTERNAL = "/ccd/internal/searchCases?ctid=CT_MasterCase";
    private static final String INVALID_PATH = "/ccd/invalid?ctid=CT_MasterCase";
    private static final String VALID_NOT_ALLOWED_PATH = "/ccd/notallowed/searchCases?ctid=CT_MasterCase";
    private static final String ES_QUERY = "{\"query\": {\"match_all\": {}},\"size\": 50}";
    private static final String SERVICE_NAME = "xui_webapp";
    private static final String BEARER = "Bearer ";

    @DisplayName("SpringCloudGateway successfully forwards /ccd/searchCases request to the data store with a "
        + "system user token and aac_manage_case_assignment client id")
    @Test
    void shouldReturn200_whenTheSearchCasesRequestHasCcdPrefix() throws Exception {

        stubSearchCase(CASE_TYPE_ID, ES_QUERY, caseDetails());

        String s2SToken = generateDummyS2SToken(SERVICE_NAME);
        this.mockMvc.perform(post(PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .header(SERVICE_AUTHORIZATION, BEARER + s2SToken)
            .content(ES_QUERY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cases.length()", is(1)))
            .andExpect(jsonPath("$.cases[0].id", is(TestFixtures.CASE_ID)));

        verify(postRequestedFor(urlEqualTo("/o/token"))
                   .withRequestBody(containing("username=master.caa%40gmail.com")));
        verify(postRequestedFor(urlEqualTo("/s2s/lease"))
                   .withRequestBody(containing("aac_manage_case_assignment")));
        verify(postRequestedFor(urlEqualTo("/searchCases?ctid=CT_MasterCase"))
                   .withHeader("Authorization",
                               containing("Bearer eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODM0NDUyOTd9aa")
                   ));
    }

    @DisplayName("SpringCloudGateway successfully forwards /ccd/internal/searchCases request to the data store with"
        + " a system user token and aac_manage_case_assignment client id")
    @Test
    void shouldReturn200_whenTheInternalSearchCasesRequestHasCcdPrefix() throws Exception {

        String s2SToken = generateDummyS2SToken(SERVICE_NAME);
        stubSearchCaseWithPrefix(CASE_TYPE_ID, ES_QUERY, caseDetails(), "/internal");

        this.mockMvc.perform(post(PATH_INTERNAL)
            .contentType(MediaType.APPLICATION_JSON)
            .header(SERVICE_AUTHORIZATION, BEARER + s2SToken)
            .content(ES_QUERY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cases.length()", is(1)))
            .andExpect(jsonPath("$.cases[0].id", is(TestFixtures.CASE_ID)));

        verify(postRequestedFor(urlEqualTo("/o/token"))
                   .withRequestBody(containing("username=master.caa%40gmail.com")));
        verify(postRequestedFor(urlEqualTo("/s2s/lease"))
                   .withRequestBody(containing("aac_manage_case_assignment")));
        verify(postRequestedFor(urlEqualTo("/internal/searchCases?ctid=CT_MasterCase"))
                   .withHeader("Authorization",
                               containing("Bearer eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODM0NDUyOTd9aa")
                   ));
    }

    @DisplayName("SpringCloudGateway fails with 404 on invalid /ccd/invalid request url")
    @Test
    void shouldReturn404_whenInvalidRequestUrlHasCcdPrefix() throws Exception {
        String s2SToken = generateDummyS2SToken(SERVICE_NAME);
        this.mockMvc.perform(post(INVALID_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .header(SERVICE_AUTHORIZATION, BEARER + s2SToken))
            .andExpect(status().isNotFound());
    }

    @DisplayName("SpringCloudGateway fails with 403 on a valid not allowed request url")
    @Test
    void shouldReturn403_whenValidNotAllowedRequestUrl() throws Exception {
        String s2SToken = generateDummyS2SToken(SERVICE_NAME);
        stubSearchCaseWithPrefix(CASE_TYPE_ID, ES_QUERY, caseDetails(), "/notallowed");

        this.mockMvc.perform(post(VALID_NOT_ALLOWED_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .header(SERVICE_AUTHORIZATION, BEARER + s2SToken)
            .content("{}"))
            .andExpect(status().isForbidden());
    }

    @DisplayName("SpringCloudGateway fails with 403 on invalid service name")
    @Test
    void shouldReturn403_whenInvalidServiceName() throws Exception {
        String s2SToken = generateDummyS2SToken("invalidService");

        this.mockMvc.perform(post(PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .header(SERVICE_AUTHORIZATION, BEARER + s2SToken)
            .content(ES_QUERY))
            .andExpect(status().isForbidden());
    }

    @SuppressWarnings("PMD.LawOfDemeter")
    public static String generateDummyS2SToken(String serviceName) {
        return Jwts.builder()
                .subject(serviceName)
                .issuedAt(new Date())
                .signWith(Jwts.SIG.HS256.key().build())
                .compact();
    }
}

