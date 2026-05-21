package uk.gov.hmcts.reform.managecase.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.managecase.BaseIT;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRolesRequest;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentRequestResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentServiceHelper;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ORGANISATION_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ORGANISATION_POLICY_ERROR;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubIdamGetUserById;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetUsersByOrganisationExternal;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.caseDetails;
import static uk.gov.hmcts.reform.managecase.TestFixtures.IdamFixture.userDetails;

class CaseAssignedUserRolesControllerIT extends BaseIT {

    private static final String CASE_USERS_PATH = "/case-users";
    private static final String CASE_ID = "4444333322221111";
    private static final String USER_ID = "123";
    private static final String CASE_ROLE = "[DEFENDANT]";
    private static final String MATCHING_ORG_ID = "ORG1";
    private static final String DIFFERENT_ORG_ID = "ORG2";
    private static final String S2S_TOKEN =
        "Bearer eyJhbGciOiJub25lIn0.eyJzdWIiOiJhYWNfbWFuYWdlX2Nhc2VfYXNzaWdubWVudCJ9.";

    @MockitoBean
    private DataStoreApiClient dataStoreApiClient;

    @MockitoBean
    private RoleAssignmentServiceHelper roleAssignmentServiceHelper;

    @Test
    @DisplayName("POST /case-users returns 201 when organisation_id matches caller PRD organisation"
        + " and case organisation policies include the caller organisation")
    void shouldReturn201WhenPostedOrganisationIdMatchesInvokerPrdOrganisation() throws Exception {
        stubGetUsersByOrganisationExternal(
            new FindUsersByOrganisationResponse(Collections.emptyList(), MATCHING_ORG_ID)
        );
        when(dataStoreApiClient.getCaseDetailsByCaseIdViaExternalApi(anyString(), eq(CASE_ID)))
            .thenReturn(stubbedCaseDetails(CASE_ID, MATCHING_ORG_ID));
        when(roleAssignmentServiceHelper.findRoleAssignmentsByCasesAndUsers(List.of(CASE_ID), List.of(USER_ID)))
            .thenReturn(RoleAssignmentResponse.builder().roleAssignments(List.of()).build());
        when(roleAssignmentServiceHelper.createRoleAssignment(any()))
            .thenReturn(RoleAssignmentRequestResponse.builder().build());
        doNothing().when(dataStoreApiClient).updateCaseSupplementaryData(anyString(), eq(CASE_ID), any());
        stubIdamGetUserById(USER_ID, userDetails(USER_ID, "caseworker-AUTOTEST2-solicitor"));

        CaseAssignedUserRolesRequest request = new CaseAssignedUserRolesRequest(List.of(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID, USER_ID, CASE_ROLE, MATCHING_ORG_ID)
        ));

        mockMvc.perform(post(CASE_USERS_PATH)
                .header("ServiceAuthorization", S2S_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status_message", is("Case-User-Role assignments created successfully")));
    }

    @Test
    @DisplayName("POST /case-users returns 400 when organisation_id differs from caller PRD organisation")
    void shouldReturn400WhenPostedOrganisationIdDiffersFromInvokerPrdOrganisation() throws Exception {
        stubGetUsersByOrganisationExternal(
            new FindUsersByOrganisationResponse(Collections.emptyList(), MATCHING_ORG_ID)
        );

        CaseAssignedUserRolesRequest request = new CaseAssignedUserRolesRequest(List.of(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID, USER_ID, CASE_ROLE, DIFFERENT_ORG_ID)
        ));

        mockMvc.perform(post(CASE_USERS_PATH)
                .header("ServiceAuthorization", S2S_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString(ORGANISATION_ID_INVALID)));
    }

    @Test
    @DisplayName("POST /case-users returns 400 when organisation_id is omitted")
    void shouldReturn400WhenPostedOrganisationIdIsMissing() throws Exception {
        stubGetUsersByOrganisationExternal(
            new FindUsersByOrganisationResponse(Collections.emptyList(), MATCHING_ORG_ID)
        );

        CaseAssignedUserRolesRequest request = new CaseAssignedUserRolesRequest(List.of(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID, USER_ID, CASE_ROLE)
        ));

        mockMvc.perform(post(CASE_USERS_PATH)
                .header("ServiceAuthorization", S2S_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString(ORGANISATION_ID_INVALID)));
    }

    @Test
    @DisplayName("POST /case-users returns 400 when caller organisation is not present on case organisation policies")
    void shouldReturn400WhenCallerOrganisationNotPresentOnCasePolicies() throws Exception {
        stubGetUsersByOrganisationExternal(
            new FindUsersByOrganisationResponse(Collections.emptyList(), MATCHING_ORG_ID)
        );
        when(dataStoreApiClient.getCaseDetailsByCaseIdViaExternalApi(anyString(), eq(CASE_ID)))
            .thenReturn(stubbedCaseDetails(CASE_ID, DIFFERENT_ORG_ID));

        CaseAssignedUserRolesRequest request = new CaseAssignedUserRolesRequest(List.of(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID, USER_ID, CASE_ROLE, MATCHING_ORG_ID)
        ));

        mockMvc.perform(post(CASE_USERS_PATH)
                .header("ServiceAuthorization", S2S_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", is(ORGANISATION_POLICY_ERROR)));
    }

    private CaseDetails stubbedCaseDetails(String caseId, String organisationId) {
        CaseDetails fixture = caseDetails(organisationId, CASE_ROLE);
        return CaseDetails.builder()
            .id(caseId)
            .caseTypeId(fixture.getCaseTypeId())
            .jurisdiction(fixture.getJurisdiction())
            .state(fixture.getState())
            .data(fixture.getData())
            .build();
    }
}
