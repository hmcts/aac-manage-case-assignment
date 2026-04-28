package uk.gov.hmcts.reform.managecase.api.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.managecase.BaseIT;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRolesRequest;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ORGANISATION_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetUsersByOrganisationExternal;

class CaseAssignedUserRolesControllerIT extends BaseIT {

    private static final String CASE_USERS_PATH = "/case-users";
    private static final String CASE_ID = "4444333322221111";
    private static final String USER_ID = "123";
    private static final String CASE_ROLE = "[DEFENDANT]";
    private static final String MATCHING_ORG_ID = "ORG1";
    private static final String DIFFERENT_ORG_ID = "ORG2";
    private static final String S2S_TOKEN =
        "Bearer eyJhbGciOiJub25lIn0.eyJzdWIiOiJhYWNfbWFuYWdlX2Nhc2VfYXNzaWdubWVudCJ9.";

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
}
