package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.managecase.BaseTest;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.api.controller.V1.MediaType.CASE_ASSIGNMENT_RESPONSE;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetUserByIdWithRoles;
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
    private static final String JURISDICTION = "AUTOTEST1";
    public static final String ORG_POLICY_ROLE = "caseworker-probate";
    public static final String ORGANIZATION_ID = "dummyOrg";

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
        stubGetUserByIdWithRoles(ASSIGNEE_ID, "caseworker-AUTOTEST1-solicitor");
        stubGetUsersByOrganisation(usersByOrganisation(professionalUsers(ASSIGNEE_ID, ANOTHER_USER)));
        stubSearchCase(CASE_TYPE_ID, CASE_ID, caseDetails(ORGANIZATION_ID, ORG_POLICY_ROLE));
    }

    @DisplayName("CAA successfully sharing case access with another solicitor in their org")
    @Test
    void shouldAssignCaseAccess_whenCAASuccessfullyShareACase() throws Exception {

        stubInvokerWithRoles(CASEWORKER_CAA);

        this.mockMvc.perform(put(PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(CASE_ASSIGNMENT_RESPONSE))
            .andExpect(jsonPath("$.status_message", is("caseworker-probate")));
    }

    @DisplayName("Solicitor successfully sharing case access with another solicitor in their org")
    @Test
    void shouldAssignCaseAccess_whenSolicitorSuccessfullyShareACase() throws Exception {

        stubInvokerWithRoles("caseworker-AUTOTEST1-solicitor");

        this.mockMvc.perform(put(PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(CASE_ASSIGNMENT_RESPONSE))
            .andExpect(jsonPath("$.status_message", is("caseworker-probate")));
    }

    @DisplayName("Must return 400 bad request response if assignee doesn't exist in invoker's organisation")
    @Test
    void shouldReturn400_whenAssigneeNotExistsInInvokersOrg() throws Exception {

        stubGetUsersByOrganisation(usersByOrganisation(professionalUsers(ANOTHER_USER)));

        this.mockMvc.perform(put(PATH)
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

        stubGetUserByIdWithRoles(ASSIGNEE_ID, "caseworker-JUD2-solicitor");

        this.mockMvc.perform(put(PATH)
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

        this.mockMvc.perform(put(PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.message",
                is("The user is neither a case access administrator nor a solicitor with access to"
                       + " the jurisdiction of the case.")));
    }

    @DisplayName("Must return 400 bad request response if invoker's organisation is not present"
        + " in the case data organisation policies")
    @Test
    void shouldReturn400_whenInvokersOrgIsNotPresentInCaseData() throws Exception {

        // TODO : fix with correct stubbing after service implementation.
        stubGetUsersByOrganisation(usersByOrganisation(professionalUsers(ANOTHER_USER)));

        this.mockMvc.perform(put(PATH)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message",
                                is("Intended assignee has to be in the same organisation as that of the invoker.")));
    }

    // Move these to individual Fixtures / Test builders
    private CaseDetails caseDetails(String organizationId, String orgPolicyRole) {
        return CaseDetails.builder()
            .caseTypeId(CASE_TYPE_ID)
            .reference(CASE_ID)
            .jurisdiction(JURISDICTION)
            .data(Maps.newHashMap("OrganisationPolicy1", jsonNode(organizationId, orgPolicyRole)))
            .build();
    }

    private JsonNode jsonNode(String organizationId, String orgPolicyRole) {
        OrganisationPolicy policy = OrganisationPolicy.builder()
            .orgPolicyCaseAssignedRole(orgPolicyRole)
            .organisation(new Organisation(organizationId, organizationId))
            .build();
        return objectMapper.convertValue(policy, JsonNode.class);
    }

    private FindUsersByOrganisationResponse usersByOrganisation(List<ProfessionalUser> users) {
        return FindUsersByOrganisationResponse.builder().users(users).build();
    }

    private List<ProfessionalUser> professionalUsers(String... userIdentifiers) {
        return Stream.of(userIdentifiers)
            .map(u -> ProfessionalUser.builder()
                .userIdentifier(u)
                .build())
            .collect(Collectors.toList());
    }

}
