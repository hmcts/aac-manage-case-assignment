package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.managecase.BaseTest;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;
import uk.gov.hmcts.reform.managecase.api.payload.OrganisationsAssignedUsersResetRequest;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.ORGS_ASSIGNED_USERS_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.PARAM_CASE_ID;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.PARAM_CASE_LIST;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.PARAM_DRY_RUN_FLAG;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.PROPERTY_CONTROLLER_ENABLED;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.PROPERTY_CONTROLLER_SAVE_ALLOWED;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.RESET_FOR_A_CASE_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.RESET_FOR_MULTIPLE_CASES_PATH;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.SAVE_NOT_ALLOWED;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.S2S_TOKEN;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

@SuppressWarnings({"squid:S100"})
@TestPropertySource(properties = {
    PROPERTY_CONTROLLER_ENABLED + "=true",
    PROPERTY_CONTROLLER_SAVE_ALLOWED + "=true"
})
public class OrganisationsAssignedUsersControllerIT extends BaseTest {

    private static final String JSON_PATH_ERRORS = "$.errors";
    private static final String JSON_PATH_MESSAGE = "$.message";

    private static final String CASE_ID_GOOD = "4444333322221111";
    private static final String CASE_ID_INVALID_LENGTH = "8010964826";
    private static final String CASE_ID_INVALID_LUHN = "4444333322221112";

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;


    @Nested
    @DisplayName("OrganisationsAssignedUsers Endpoint Disabled")
    @TestPropertySource(properties = {
        PROPERTY_CONTROLLER_ENABLED + "=false",
        PROPERTY_CONTROLLER_SAVE_ALLOWED + "=true"
    })
    class OrganisationsAssignedUsersEndpointDisabled {

        @Inject // NB: using a fresh WAC instance, so it loads using new test.properties
        private WebApplicationContext wac;

        @BeforeEach
        public void setUp() {
            setUpMvc(wac);
        }

        @Test
        @DisplayName("Should return NOT_FOUND for RESET_FOR_A_CASE_PATH if controller is disabled")
        void shouldReturn404_forResetForACase_whenControllerIsDisabled() throws Exception {

            // GIVEN
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_CASE_ID, CASE_ID_GOOD);
            queryParams.add(PARAM_DRY_RUN_FLAG, "true");

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                .queryParams(queryParams)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return NOT_FOUND for RESET_FOR_MULTIPLE_CASES_PATH if controller is disabled")
        void shouldReturn404_forResetForMultipleCases_whenControllerIsDisabled() throws Exception {

            // GIVEN
            OrganisationsAssignedUsersResetRequest request = new OrganisationsAssignedUsersResetRequest(
                List.of(CASE_ID_GOOD),
                true
            );

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_MULTIPLE_CASES_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                .content(mapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isNotFound());
        }
    }


    @Nested
    @DisplayName("OrganisationsAssignedUsers Endpoint Disabled")
    @TestPropertySource(properties = {
        PROPERTY_CONTROLLER_ENABLED + "=true",
        PROPERTY_CONTROLLER_SAVE_ALLOWED + "=false"
    })
    class OrganisationsAssignedUsersSaveDisabled {

        @Inject // NB: using a fresh WAC instance, so it loads using new test.properties
        private WebApplicationContext wac;

        @BeforeEach
        public void setUp() {
            setUpMvc(wac);
        }

        @Test
        @DisplayName("Should return FORBIDDEN for RESET_FOR_A_CASE_PATH if not a dry run but save is disabled")
        void shouldReturn403_forResetForACase_whenNotDryRunButSaveDisabled() throws Exception {

            // GIVEN
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_CASE_ID, CASE_ID_GOOD);
            queryParams.add(PARAM_DRY_RUN_FLAG, "false");

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                .queryParams(queryParams)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, is(SAVE_NOT_ALLOWED)));
        }

        @Test
        @DisplayName("Should return FORBIDDEN for RESET_FOR_MULTIPLE_CASES_PATH if not a dry run but save is disabled")
        void shouldReturn403_forResetForMultipleCases_whenNotDryRunButSaveDisabled() throws Exception {

            // GIVEN
            OrganisationsAssignedUsersResetRequest request = new OrganisationsAssignedUsersResetRequest(
                List.of(CASE_ID_GOOD),
                false
            );

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_MULTIPLE_CASES_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                .content(mapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, is(SAVE_NOT_ALLOWED)));
        }
    }


    @Nested
    @DisplayName("POST " + ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
    class RestOrganisationsAssignedUsersCountDataForACase {

        @BeforeEach
        public void setUp() {
            setUpMvc(wac);
        }

        @ParameterizedTest(name = "Should return 400 when bad Case ID passed: {0}")
        @MethodSource(BAD_CASE_IDS_AND_ERRORS)
        void shouldReturn400_whenBadCaseIdPassed(String caseId, String expectedError) throws Exception {

            // GIVEN
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_CASE_ID, caseId);
            queryParams.add(PARAM_DRY_RUN_FLAG, "true");

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                .queryParams(queryParams)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
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
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                .queryParams(queryParams)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isBadRequest());
        }

        @DisplayName("Should return 400 when Case ID not supplied")
        @Test
        void shouldReturn400_whenMissingCaseId() throws Exception {

            // GIVEN
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_DRY_RUN_FLAG, "true");

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                .queryParams(queryParams)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isBadRequest());
        }

        @DisplayName("Should return 400 when DryRun flag not supplied")
        @Test
        void shouldReturn400_whenMissingDryRunFlag() throws Exception {

            // GIVEN
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_CASE_ID, CASE_ID_GOOD);

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                .queryParams(queryParams)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isBadRequest());
        }
    }


    @Nested
    @DisplayName("POST " + ORGS_ASSIGNED_USERS_PATH + RESET_FOR_MULTIPLE_CASES_PATH)
    class RestOrganisationsAssignedUsersCountDataForMultipleCases {

        @BeforeEach
        public void setUp() {
            setUpMvc(wac);
        }

        @ParameterizedTest(name = "Should return 400 when bad Case List value passed: {0}")
        @MethodSource(BAD_CASE_IDS_AND_ERRORS)
        void shouldReturn400_whenBadCaseListValuePassed(String caseId, String expectedError) throws Exception {

            // GIVEN
            OrganisationsAssignedUsersResetRequest request = new OrganisationsAssignedUsersResetRequest(
                List.of(caseId),
                true
            );

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_MULTIPLE_CASES_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                .content(mapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
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
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_MULTIPLE_CASES_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                .content(request)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isBadRequest());
        }

        @DisplayName("Should return 400 when empty Case List passed")
        @Test
        void shouldReturn400_whenEmptyCaseListPassed() throws Exception {

            // GIVEN
            OrganisationsAssignedUsersResetRequest request = new OrganisationsAssignedUsersResetRequest(
                List.of(),
                true
            );

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_MULTIPLE_CASES_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                .content(mapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_ERRORS, hasItem(ValidationError.EMPTY_CASE_ID_LIST)));
        }

        @DisplayName("Should return 400 when Case List not supplied")
        @Test
        void shouldReturn400_whenMissingCaseList() throws Exception {

            // GIVEN
            String request = "{ \"" + PARAM_DRY_RUN_FLAG + "\": \"true\" }";

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_MULTIPLE_CASES_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                .content(request)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isBadRequest());
        }

        @DisplayName("Should return 400 when DryRun flag not supplied")
        @Test
        void shouldReturn400_whenMissingDryRunFlag() throws Exception {

            // GIVEN
            String request = "{ \"" + PARAM_CASE_LIST + "\": [ \"" + CASE_ID_GOOD + "\" ] }";

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_MULTIPLE_CASES_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN)
                                .content(mapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isBadRequest());
        }
    }


    private static final String BAD_CASE_IDS_AND_ERRORS
        = "uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersControllerIT#badCaseIdAndErrors";

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

    private void setUpMvc(WebApplicationContext wac) {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

}
