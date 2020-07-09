package uk.gov.hmcts.reform.managecase.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.managecase.BaseTest;

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

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.MethodNamingConventions"})
public class ZuulProxyDataStoreRequestIT extends BaseTest {
    private static final String CASE_TYPE_ID = "CT_MasterCase";
    private static final String PATH = "/ccd/searchCases?ctid=CT_MasterCase";
    private static final String INVALID_PATH = "/ccd/invalid?ctid=CT_MasterCase";
    private static final String VALID_NOT_WHITELISTED_PATH = "/ccd/notwhitelisted/searchCases?ctid=CT_MasterCase";
    private static final String ORG_POLICY_ROLE = "caseworker-probate";
    private static final String ORGANIZATION_ID = "TEST_ORG";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        stubSearchCase(CASE_TYPE_ID, caseDetails(ORGANIZATION_ID, ORG_POLICY_ROLE));
    }

    @DisplayName("Zuul successfully forwards /ccd/searchCases request to the data store")
    @Test
    void shouldReturn200_whenTheRequestHasCcdPrefix() throws Exception {

        this.mockMvc.perform(post(PATH)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .content("{\"query\": {\"match_all\": {}},\"size\": 50}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cases.length()", is(1)))
            .andExpect(jsonPath("$.cases[0].reference", is("12345678")));
    }

    @DisplayName("Zuul forwards /ccd/searchCases to the data store using a system user token"
        + " and aac_manage_case_assignment client id")
    @Test
    void shouldReturn200_withASystemUserTokenWhenTheRequestHasCcdPrefixAndAnotherUserToken() throws Exception {

        this.mockMvc.perform(post(PATH)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .content("{\"query\": {\"match_all\": {}},\"size\": 50}"))
            .andExpect(status().isOk());

        verify(postRequestedFor(urlEqualTo("/o/token"))
                   .withRequestBody(containing("username=master.solicitor.1%40gmail.com")));
        verify(postRequestedFor(urlEqualTo("/s2s/lease"))
                   .withRequestBody(containing("aac_manage_case_assignment")));
        verify(postRequestedFor(urlEqualTo("/searchCases?ctid=CT_MasterCase"))
                   .withHeader("Authorization",
                               containing("Bearer eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODM0NDUyOTd9aa")
                   ));
    }

    @DisplayName("Zuul fails with 404 on invalid /ccd/invalid request url")
    @Test
    void shouldReturn404_whenInvalidRequestUrlHasCcdPrefix() throws Exception {

        this.mockMvc.perform(post(INVALID_PATH)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .content("{}"))
            .andExpect(status().isNotFound());
    }

    @DisplayName("Zuul fails with 400 on a valid not whitelisted request url")
    @Test
    void shouldReturn404_whenValidNotWhitelistedRequestUrl() throws Exception {

        stubSearchCaseWithPrefix(CASE_TYPE_ID, caseDetails(ORGANIZATION_ID, ORG_POLICY_ROLE), "/notwhitelisted");

        this.mockMvc.perform(post(VALID_NOT_WHITELISTED_PATH)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .content("{}"))
            .andExpect(status().isBadRequest());
    }
}

