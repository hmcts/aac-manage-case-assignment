package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.managecase.BaseTest;
import uk.gov.hmcts.reform.managecase.TestFixtures;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.caseDetails;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.user;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.usersByOrganisation;
import static uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController.ASSIGN_ACCESS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController.GET_ASSIGNMENTS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubAssignCase;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseAssignments;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetUsersByOrganisation;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubSearchCase;
import static uk.gov.hmcts.reform.managecase.service.CaseAssignmentService.DUMMY_TITLE;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.MethodNamingConventions",
    "PMD.AvoidDuplicateLiterals"})
public class CaseAssignmentControllerIT extends BaseTest {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String ASSIGNEE_ID = "ae2eb34c-816a-4eea-b714-6654d022fcef";
    private static final String ANOTHER_USER = "vcd345cvs-816a-4eea-b714-6654d022fcef";
    private static final String CASE_ID = "1588234985453946";
    private static final String ORG_POLICY_ROLE = "caseworker-probate";
    private static final String ORG_POLICY_ROLE2 = "caseworker-probate2";
    private static final String ORGANIZATION_ID = "TEST_ORG";

    private static final String RAW_QUERY = "{\"query\":{\"bool\":{\"filter\":{\"term\":{\"reference\":%s}}}}}";
    private static final String ES_QUERY = String.format(RAW_QUERY, CASE_ID);

    public static final String PATH = "/case-assignments";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CaseAssignmentRequest request;

    @BeforeEach
    void setUp() {
        request = new CaseAssignmentRequest(CASE_TYPE_ID, CASE_ID, ASSIGNEE_ID);
        // Positive stub mappings - individual tests override again for a specific scenario.
        stubGetUsersByOrganisation(usersByOrganisation(user(ASSIGNEE_ID, "caseworker-AUTOTEST1-solicitor"),
                user(ANOTHER_USER)));
        stubSearchCase(CASE_TYPE_ID, ES_QUERY, caseDetails(ORGANIZATION_ID, ORG_POLICY_ROLE));
        stubAssignCase(CASE_ID, ASSIGNEE_ID, ORG_POLICY_ROLE);
    }

    @DisplayName("Invoker successfully sharing case access with another solicitor in their org")
    @Test
    void shouldAssignCaseAccess_whenInvokerSuccessfullyShareACase() throws Exception {

        this.mockMvc.perform(post(PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.status_message", is(String.format(ASSIGN_ACCESS_MESSAGE, ORG_POLICY_ROLE))));

        verify(postRequestedFor(urlEqualTo("/case-users")));
    }

    @DisplayName("Successfully sharing case access with multiple org roles")
    @Test
    void shouldAssignCaseAccess_withMultipleOrganisationRoles() throws Exception {

        stubSearchCase(CASE_TYPE_ID, ES_QUERY, caseDetails(ORGANIZATION_ID, ORG_POLICY_ROLE, ORG_POLICY_ROLE2));
        stubAssignCase(CASE_ID, ASSIGNEE_ID, ORG_POLICY_ROLE, ORG_POLICY_ROLE2);

        this.mockMvc.perform(post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status_message", is(String.format(ASSIGN_ACCESS_MESSAGE,
                        StringUtils.join(List.of(ORG_POLICY_ROLE, ORG_POLICY_ROLE2), ',')))));

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

    @DisplayName("Must return 400 bad request response if invoker's organisation is not present"
        + " in the case data organisation policies")
    @Test
    void shouldReturn400_whenInvokersOrgIsNotPresentInCaseData() throws Exception {

        stubSearchCase(CASE_TYPE_ID, ES_QUERY, caseDetails("ANOTHER_ORGANIZATION_ID", ORG_POLICY_ROLE));

        this.mockMvc.perform(post(PATH)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is("Case ID has to be one for which a case role"
                    + " is represented by the invoker's organisation.")));
    }

    @DisplayName("Successfully return case assignments of my organisation")
    @Test
    void shouldGetCaseAssignments_forAValidRequest() throws Exception {

        CaseUserRole caseUserRole = CaseUserRole.builder()
                .caseId(CASE_ID)
                .userId(ASSIGNEE_ID)
                .caseRole(TestFixtures.CASE_ROLE)
                .build();

        stubGetUsersByOrganisation(usersByOrganisation(user(ASSIGNEE_ID)));

        stubGetCaseAssignments(List.of(CASE_ID), List.of(ASSIGNEE_ID), List.of(caseUserRole));

        this.mockMvc.perform(get(PATH)
                .queryParam("case_ids", CASE_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))

                .andExpect(jsonPath("$.status_message", is(GET_ASSIGNMENTS_MESSAGE)))

                .andExpect(jsonPath("$.case_assignments", hasSize(1)))
                .andExpect(jsonPath("$.case_assignments[0].case_id", is(CASE_ID)))
                .andExpect(jsonPath("$.case_assignments[0].case_title", is(CASE_ID + "-" + DUMMY_TITLE)))
                .andExpect(jsonPath("$.case_assignments[0].shared_with", hasSize(1)))
                .andExpect(jsonPath("$.case_assignments[0].shared_with[0].first_name", is(TestFixtures.FIRST_NAME)))
                .andExpect(jsonPath("$.case_assignments[0].shared_with[0].last_name", is(TestFixtures.LAST_NAME)))
                .andExpect(jsonPath("$.case_assignments[0].shared_with[0].email", is(TestFixtures.EMAIL)))
                .andExpect(jsonPath("$.case_assignments[0].shared_with[0].case_roles", hasSize(1)))
                .andExpect(jsonPath("$.case_assignments[0].shared_with[0].case_roles[0]", is(TestFixtures.CASE_ROLE)));
    }

}
