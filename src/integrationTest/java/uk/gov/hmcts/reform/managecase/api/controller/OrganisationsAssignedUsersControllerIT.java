package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.gov.hmcts.reform.managecase.BaseTest;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;
import uk.gov.hmcts.reform.managecase.api.payload.OrganisationAssignedUsersResetRequest;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationAssignedUsersController.ORG_ASSIGNED_USERS_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationAssignedUsersController.PARAM_CASE_ID;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationAssignedUsersController.PARAM_CASE_LIST;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationAssignedUsersController.PARAM_DRY_RUN_FLAG;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationAssignedUsersController.RESET_ORG_COUNTS_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationAssignedUsersController.RESET_ORG_COUNT_PATH;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.S2S_TOKEN;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

@SuppressWarnings({"squid:S100"})
public class OrganisationAssignedUsersControllerIT {

    private static final String JSON_PATH_ERRORS = "$.errors";

    private static final String CASE_ID_GOOD = "4444333322221111";
    private static final String CASE_ID_INVALID_LENGTH = "8010964826";
    private static final String CASE_ID_INVALID_LUHN = "4444333322221112";

    @Nested
    @DisplayName("POST " + ORG_ASSIGNED_USERS_PATH + RESET_ORG_COUNT_PATH)
    class RestOrganisationCountForCase extends BaseTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @ParameterizedTest(name = "Should return 400 when bad Case ID passed: {0}")
        @MethodSource(BAD_CASE_IDS_AND_ERRORS)
        void shouldReturn400_whenBadCaseIdPassed(String caseId, String expectedError) throws Exception {

            // GIVEN
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_CASE_ID, caseId);
            queryParams.add(PARAM_DRY_RUN_FLAG, "true");

            // WHEN
            this.mockMvc.perform(post(ORG_ASSIGNED_USERS_PATH + RESET_ORG_COUNT_PATH)
                                     .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                     .queryParams(queryParams)
                                     .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_ERRORS, hasItem(expectedError)));
        }

        @DisplayName("Should return 400 when bad DryRun flag passed")
        @Test
        void shouldReturn400_whenBadDryRunFlagPassed() throws Exception {

            // GIVEN
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_DRY_RUN_FLAG, "bad-value");

            // WHEN
            this.mockMvc.perform(post(ORG_ASSIGNED_USERS_PATH + RESET_ORG_COUNT_PATH)
                                     .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                     .queryParams(queryParams)
                                     .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }

        @DisplayName("Should return 400 when Case ID not supplied")
        @Test
        void shouldReturn400_whenMissingCaseId() throws Exception {

            // GIVEN
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_DRY_RUN_FLAG, "true");

            // WHEN
            this.mockMvc.perform(post(ORG_ASSIGNED_USERS_PATH + RESET_ORG_COUNT_PATH)
                                     .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                     .queryParams(queryParams)
                                     .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }

        @DisplayName("Should return 400 when DryRun flag not supplied")
        @Test
        void shouldReturn400_whenMissingDryRunFlag() throws Exception {

            // GIVEN
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_CASE_ID, CASE_ID_GOOD);

            // WHEN
            this.mockMvc.perform(post(ORG_ASSIGNED_USERS_PATH + RESET_ORG_COUNT_PATH)
                                     .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                     .queryParams(queryParams)
                                     .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST " + ORG_ASSIGNED_USERS_PATH + RESET_ORG_COUNTS_PATH)
    class RestOrganisationCountForMultipleCases extends BaseTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper mapper;

        @ParameterizedTest(name = "Should return 400 when bad Case List value passed: {0}")
        @MethodSource(BAD_CASE_IDS_AND_ERRORS)
        void shouldReturn400_whenBadCaseListValuePassed(String caseId, String expectedError) throws Exception {

            // GIVEN
            OrganisationAssignedUsersResetRequest request = new OrganisationAssignedUsersResetRequest(
                List.of(caseId),
                true
            );

            // WHEN
            this.mockMvc.perform(post(ORG_ASSIGNED_USERS_PATH + RESET_ORG_COUNTS_PATH)
                                     .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                     .content(mapper.writeValueAsString(request))
                                     .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_ERRORS, hasItem(expectedError)));
        }

        @DisplayName("Should return 400 when bad DryRun flag passed")
        @Test
        void shouldReturn400_whenBadDryRunFlagPassed() throws Exception {

            // GIVEN
            String request = "{ \"" + PARAM_CASE_LIST + "\": [ \"" + CASE_ID_GOOD + "\" ], "
                + "\"" + PARAM_DRY_RUN_FLAG + "\": \"bad-value\" }";

            // WHEN
            this.mockMvc.perform(post(ORG_ASSIGNED_USERS_PATH + RESET_ORG_COUNTS_PATH)
                                     .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                     .content(request)
                                     .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }

        @DisplayName("Should return 400 when empty Case List passed")
        @Test
        void shouldReturn400_whenEmptyCaseListPassed() throws Exception {

            // GIVEN
            OrganisationAssignedUsersResetRequest request = new OrganisationAssignedUsersResetRequest(
                List.of(),
                true
            );

            // WHEN
            this.mockMvc.perform(post(ORG_ASSIGNED_USERS_PATH + RESET_ORG_COUNTS_PATH)
                                     .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                     .content(mapper.writeValueAsString(request))
                                     .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_ERRORS, hasItem(ValidationError.EMPTY_CASE_ID_LIST)));
        }

        @DisplayName("Should return 400 when Case List not supplied")
        @Test
        void shouldReturn400_whenMissingCaseList() throws Exception {

            // GIVEN
            String request = "{ \"" + PARAM_DRY_RUN_FLAG + "\": \"true\" }";

            // WHEN
            this.mockMvc.perform(post(ORG_ASSIGNED_USERS_PATH + RESET_ORG_COUNTS_PATH)
                                     .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                     .content(request)
                                     .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }

        @DisplayName("Should return 400 when DryRun flag not supplied")
        @Test
        void shouldReturn400_whenMissingDryRunFlag() throws Exception {

            // GIVEN
            String request = "{ \"" + PARAM_CASE_LIST + "\": [ \"" + CASE_ID_GOOD + "\" ] }";

            // WHEN
            this.mockMvc.perform(post(ORG_ASSIGNED_USERS_PATH + RESET_ORG_COUNTS_PATH)
                                     .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                     .content(mapper.writeValueAsString(request))
                                     .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        }
    }

    private static final String BAD_CASE_IDS_AND_ERRORS
        = "uk.gov.hmcts.reform.managecase.api.controller.OrganisationAssignedUsersControllerIT#badCaseIdAndErrors";

    @SuppressWarnings("unused")
    private static Stream<Arguments> badCaseIdAndErrors() {
        return Stream.of(
            // NB: params correspond to:
            // * caseId :: case ID to use in call
            // * expectedError :: expected error to find in call
            Arguments.of("", ValidationError.CASE_ID_EMPTY),
            Arguments.of(CASE_ID_INVALID_LENGTH, ValidationError.CASE_ID_INVALID_LENGTH),
            Arguments.of(CASE_ID_INVALID_LUHN, ValidationError.CASE_ID_INVALID)
        );
    }

}
