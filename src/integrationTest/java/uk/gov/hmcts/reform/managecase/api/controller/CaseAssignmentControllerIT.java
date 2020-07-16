package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.managecase.BaseTest;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentRequest;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.caseDetails;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.user;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.usersByOrganisation;
import static uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController.MESSAGE;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubAssignCase;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetUsersByOrganisation;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubInvokerWithRoles;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubSearchCase;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.MethodNamingConventions",
    "PMD.AvoidDuplicateLiterals"})
public class CaseAssignmentControllerIT extends BaseTest {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String ASSIGNEE_ID = "ae2eb34c-816a-4eea-b714-6654d022fcef";
    private static final String ANOTHER_USER = "vcd345cvs-816a-4eea-b714-6654d022fcef";
    private static final String CASE_ID = "12345678";
    private static final String ORG_POLICY_ROLE = "caseworker-probate";
    private static final String ORGANIZATION_ID = "TEST_ORG";

    public static final String PATH = "/case-assignments";
    public static final String CASEWORKER_CAA = "caseworker-caa";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CaseAssignmentRequest request;

    @BeforeEach
    void setUp() {
        request = new CaseAssignmentRequest(CASE_TYPE_ID, CASE_ID, ASSIGNEE_ID);
        // Positive stub mappings - individual tests override again for a specific scenario.
        stubInvokerWithRoles(CASEWORKER_CAA);
        stubGetUsersByOrganisation(usersByOrganisation(user(ASSIGNEE_ID, "caseworker-AUTOTEST1-solicitor"),
                user(ANOTHER_USER)));
        stubSearchCase(CASE_TYPE_ID, caseDetails(ORGANIZATION_ID, ORG_POLICY_ROLE));
        stubAssignCase(CASE_ID, ASSIGNEE_ID, ORG_POLICY_ROLE);
    }

    @DisplayName("CAA successfully sharing case access with another solicitor in their org")
    @Test
    void shouldAssignCaseAccess_whenCAASuccessfullyShareACase() throws Exception {

        stubInvokerWithRoles(CASEWORKER_CAA);

        this.mockMvc.perform(post(PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status_message", is(String.format(MESSAGE, ORG_POLICY_ROLE))));

        verify(postRequestedFor(urlEqualTo("/case-users")));
    }

    @DisplayName("Solicitor successfully sharing case access with another solicitor in their org")
    @Test
    void shouldAssignCaseAccess_whenSolicitorSuccessfullyShareACase() throws Exception {

        stubInvokerWithRoles("caseworker-AUTOTEST1-solicitor");

        this.mockMvc.perform(post(PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.status_message", is(String.format(MESSAGE, ORG_POLICY_ROLE))));

        verify(postRequestedFor(urlEqualTo("/case-users")));
    }

    @DisplayName("Must return 400 bad request response if assignee doesn't exist in invoker's organisation")
    @Test
    void shouldReturn400_whenAssigneeNotExistsInInvokersOrg() throws Exception {

        stubGetUsersByOrganisation(usersByOrganisation(user(ANOTHER_USER)));

        this.mockMvc.perform(post(PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message",
                is("Intended assignee has to be in the same organisation as that of the invoker.")));
    }

    @DisplayName("Must return 400 bad request response if assignee doesn't have a solicitor role for the"
        + " jurisdiction of the case")
    @Test
    void shouldReturn400_whenAssigneeNotHaveCorrectJurisdictionRole() throws Exception {

        stubGetUsersByOrganisation(usersByOrganisation(user(ASSIGNEE_ID, "caseworker-JUD2-solicitor")));

        this.mockMvc.perform(post(PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message",
                   is("Intended assignee has to be a solicitor enabled in the jurisdiction of the case.")));
    }

    @DisplayName("Must return 403 bad request response if the invoker doesn't have a solicitor role"
        + " for the jurisdiction of the case or a caseworker-caa role")
    @Test
    void shouldReturn403_whenInvokerDoesNotHaveRequiredRoles() throws Exception {

        stubInvokerWithRoles("caseworker-JUD2-solicitor");

        this.mockMvc.perform(post(PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message",
                is("The user is neither a case access administrator nor a solicitor with access to"
                       + " the jurisdiction of the case.")));
    }

    @Disabled
    @DisplayName("Must return 400 bad request response if invoker's organisation is not present"
        + " in the case data organisation policies")
    @Test
    void shouldReturn400_whenInvokersOrgIsNotPresentInCaseData() throws Exception {

        stubSearchCase(CASE_TYPE_ID, caseDetails("ANOTHER_ORGANIZATION_ID", ORG_POLICY_ROLE));

        this.mockMvc.perform(post(PATH)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("Case ID has to be one for which a case role"
                    + " is represented by the invoker's organisation.")));
    }

}
