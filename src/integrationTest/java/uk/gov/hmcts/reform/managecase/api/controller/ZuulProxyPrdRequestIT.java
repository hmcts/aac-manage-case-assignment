package uk.gov.hmcts.reform.managecase.api.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.TextCodec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.managecase.BaseTest;

import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ORGANIZATION_ID;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.user;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.usersByOrganisation;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetUsersByOrganisationExternal;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetUsersByOrganisationInternal;
import static uk.gov.hmcts.reform.managecase.zuulfilters.AuthHeaderRoutingFilter.SERVICE_AUTHORIZATION;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.MethodNamingConventions", "PMD.AvoidDuplicateLiterals"})
public class ZuulProxyPrdRequestIT extends BaseTest {

    private static final String PATH = "/prd/refdata/internal/v1/organisations/TEST_ORG/users?returnRoles=false";
    private static final String INVALID_PATH = "/prd/invalid";
    private static final String VALID_NOT_ALLOWED_PATH =
        "/prd/refdata/external/v1/organisations/users?status=Active&returnRoles=false";
    private static final String SERVICE_NAME = "xui_webapp";
    private static final String BEARER = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("Zuul successfully forwards prefixed valid request URL to PRD with a system user token"
        + " and aac_manage_case_assignment client id")
    @Test
    void shouldReturn200_whenGetUsersByOrganisationRequestHasPrdPrefix() throws Exception {

        stubGetUsersByOrganisationInternal(usersByOrganisation(user("User1"), user("User2")), ORGANIZATION_ID);

        String s2SToken = generateDummyS2SToken(SERVICE_NAME);
        this.mockMvc.perform(get(PATH)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .header(SERVICE_AUTHORIZATION, BEARER + s2SToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.users.length()", is(2)))
            .andExpect(jsonPath("$.organisationIdentifier", is(ORGANIZATION_ID)));

        verify(postRequestedFor(urlEqualTo("/o/token"))
                   .withRequestBody(containing("username=master.caa%40gmail.com")));
        verify(postRequestedFor(urlEqualTo("/s2s/lease"))
                   .withRequestBody(containing("aac_manage_case_assignment")));
        verify(getRequestedFor(urlEqualTo("/refdata/internal/v1/organisations/TEST_ORG/users?returnRoles=false"))
                   .withHeader("Authorization",
                               containing("Bearer eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODM0NDUyOTd9aa")
                   ));
    }

    @DisplayName("Zuul fails with 404 on invalid /prd/invalid request url")
    @Test
    void shouldReturn404_whenInvalidRequestUrlHasPrdPrefix() throws Exception {
        String s2SToken = generateDummyS2SToken(SERVICE_NAME);
        this.mockMvc.perform(get(INVALID_PATH)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .header(SERVICE_AUTHORIZATION, BEARER + s2SToken))
            .andExpect(status().isNotFound());
    }

    @DisplayName("Zuul fails with 403 on a valid not allowed request url")
    @Test
    void shouldReturn403_whenValidNotAllowedRequestUrl() throws Exception {
        String s2SToken = generateDummyS2SToken(SERVICE_NAME);
        stubGetUsersByOrganisationExternal(usersByOrganisation(user("User1")));

        this.mockMvc.perform(get(VALID_NOT_ALLOWED_PATH)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .header(SERVICE_AUTHORIZATION, BEARER + s2SToken))
            .andExpect(status().isForbidden());
    }

    @DisplayName("Zuul fails with 403 on invalid service name")
    @Test
    void shouldReturn403_whenInvalidServiceName() throws Exception {
        String s2SToken = generateDummyS2SToken("invalidService");

        this.mockMvc.perform(get(PATH)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .header(SERVICE_AUTHORIZATION, BEARER + s2SToken))
            .andExpect(status().isForbidden());
    }

    @SuppressWarnings("PMD.LawOfDemeter")
    public static String generateDummyS2SToken(String serviceName) {
        return Jwts.builder()
                .setSubject(serviceName)
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS256, TextCodec.BASE64.encode("AA"))
                .compact();
    }
}

