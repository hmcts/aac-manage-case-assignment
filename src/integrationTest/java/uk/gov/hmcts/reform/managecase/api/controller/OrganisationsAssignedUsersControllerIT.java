package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.SupplementaryDataUpdates;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DefaultDataStoreRepository;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CASE_TYPE_ID;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.organisationPolicy;
import static uk.gov.hmcts.reform.managecase.TestFixtures.JURISDICTION;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.user;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.usersByOrganisation;
import static uk.gov.hmcts.reform.managecase.TestFixtures.RoleAssignmentsFixture.roleAssignment;
import static uk.gov.hmcts.reform.managecase.TestFixtures.RoleAssignmentsFixture.roleAssignmentResponse;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.ORGS_ASSIGNED_USERS_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.PARAM_CASE_ID;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.PARAM_CASE_LIST;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.PARAM_DRY_RUN_FLAG;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.PROPERTY_CONTROLLER_ENABLED;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.PROPERTY_CONTROLLER_SAVE_ALLOWED;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.PROPERTY_S2S_AUTHORISED_FOR_ENDPOINT;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.RESET_FOR_A_CASE_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersController.RESET_FOR_MULTIPLE_CASES_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.OrganisationsAssignedUsersControllerIT.S2S_AUTHORISED_FOR_ENDPOINT;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.SAVE_NOT_ALLOWED;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubFindRoleAssignmentsByCasesAndUsers;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseViaExternalApi;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetUsersByOrganisationInternal;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubUpdateSupplementaryData;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

@SuppressWarnings({"squid:S100","checkstyle:VariableDeclarationUsageDistance"})
@TestPropertySource(properties = {
    PROPERTY_CONTROLLER_ENABLED + "=true",
    PROPERTY_CONTROLLER_SAVE_ALLOWED + "=true",
    PROPERTY_S2S_AUTHORISED_FOR_ENDPOINT + "=" +  S2S_AUTHORISED_FOR_ENDPOINT + ",another_service_name"
})
public class OrganisationsAssignedUsersControllerIT extends BaseTest {

    private static final String URL_SUPPLEMENTARY_UPDATE = "/cases/%s/supplementary-data";

    private static final String JSON_PATH_ERRORS = "$.errors";
    private static final String JSON_PATH_MESSAGE = "$.message";

    private static final String CASE_ID_GOOD = "1111222233334444";
    private static final String CASE_ID_GOOD_2 = "2222333344441111";
    private static final String CASE_ID_GOOD_3 = "3333222211114444";
    private static final String CASE_ID_INVALID_LENGTH = "8010964826";
    private static final String CASE_ID_INVALID_LUHN = "4444333322221112";
    private static final String CASE_ID_NOT_FOUND = "4444333322221111";

    private static final String ORGANISATION_ID_1 = "Org_1";
    private static final String ORGANISATION_ID_2 = "Org_2";
    private static final String ORGANISATION_ID_3 = "Org_3";
    private static final String ORGANISATION_ID_BAD_1 = "Org_BAD_1";
    private static final String ORGANISATION_ID_BAD_2 = "Org_BAD_2";
    private static final String ROLE_1 = "Role 1";
    private static final String ROLE_2 = "Role 2";
    private static final String ROLE_3 = "Role 3";
    private static final String ROLE_BAD = "Role BAD";
    private static final String USER_ID_1 = "User 1";
    private static final String USER_ID_2 = "User 2";
    private static final String USER_ID_3 = "User 3";
    private static final String USER_ID_4 = "User 4";
    private static final String USER_ID_BAD_1 = "User BAD 1";
    private static final String USER_ID_BAD_2 = "User BAD 2";

    protected static final String S2S_AUTHORISED_FOR_ENDPOINT = "service_and_endpoint";
    protected static final String S2S_NOT_AUTHORISED_FOR_ENDPOINT = "service_only";

    private static final String S2S_TOKEN_GOOD = generateS2SToken(S2S_AUTHORISED_FOR_ENDPOINT, 10000);
    private static final String S2S_TOKEN_BAD_ENDPOINT = generateS2SToken(S2S_NOT_AUTHORISED_FOR_ENDPOINT, 20000);


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
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
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
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
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
            queryParams.add(PARAM_DRY_RUN_FLAG, "false"); // i.e. not a dry run

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
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
                false // i.e. not a dry run
            );

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_MULTIPLE_CASES_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
                                .content(mapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isForbidden())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, is(SAVE_NOT_ALLOWED)));
        }

    }


    @Nested
    @DisplayName("OrganisationsAssignedUsers Service Not Authorised For Endpoint")
    class OrganisationsAssignedUsersServiceNotAuthorised {

        @Inject // NB: using a fresh WAC instance, so it loads using new test.properties
        private WebApplicationContext wac;

        @BeforeEach
        public void setUp() {
            setUpMvc(wac);
        }

        @Test
        @DisplayName("Should return FORBIDDEN for RESET_FOR_A_CASE_PATH when service not authorised for endpoint")
        void shouldReturn403_forResetForACase_whenServiceNotAuthorised() throws Exception {

            // GIVEN
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_CASE_ID, CASE_ID_GOOD);
            queryParams.add(PARAM_DRY_RUN_FLAG, "true");

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_BAD_ENDPOINT)
                                .queryParams(queryParams)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName(
            "Should return FORBIDDEN for RESET_FOR_MULTIPLE_CASES_PATH when service not authorised for endpoint"
        )
        void shouldReturn403_forResetForMultipleCases_whenServiceNotAuthorised() throws Exception {

            // GIVEN
            OrganisationsAssignedUsersResetRequest request = new OrganisationsAssignedUsersResetRequest(
                List.of(CASE_ID_GOOD),
                true
            );

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_MULTIPLE_CASES_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_BAD_ENDPOINT)
                                .content(mapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isForbidden());
        }
    }


    @Nested
    @DisplayName("POST " + ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
    class RestOrganisationsAssignedUsersCountDataForACase {

        private static final String JSON_PATH_CASE_ID = "$.case_id";
        private static final String JSON_PATH_ORGS_ASSIGNED_USERS = "$.orgs_assigned_users";
        private static final String JSON_PATH_SKIPPED_ORGS = "$.skipped_organisations";

        @BeforeEach
        public void setUp() {
            // reset wiremock counters
            WireMock.resetAllRequests();

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
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
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
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
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
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
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
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
                                .queryParams(queryParams)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isBadRequest());
        }

        @DisplayName("Should return 404 when case not found")
        @Test
        void shouldReturn404_whenCaseNotFound() throws Exception {

            // GIVEN
            stubGetCaseViaExternalApi(CASE_ID_NOT_FOUND, notFound());

            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_CASE_ID, CASE_ID_NOT_FOUND);
            queryParams.add(PARAM_DRY_RUN_FLAG, "true");

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
                                .queryParams(queryParams)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, is(ValidationError.CASE_NOT_FOUND)));
        }

        @DisplayName("Should return 200 with `orgsAssignedUsers` empty if no organisation policies")
        @Test
        void shouldReturn200_withOrgsAssignedUsersEmpty_ifNoOrgPolicies() throws Exception {

            // GIVEN
            stubCaseDetails(CASE_ID_GOOD, Collections.emptyList());

            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_CASE_ID, CASE_ID_GOOD);
            queryParams.add(PARAM_DRY_RUN_FLAG, "false");

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
                                .queryParams(queryParams)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN (part 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CASE_ID, is(CASE_ID_GOOD)))
                .andExpect(jsonPath(JSON_PATH_ORGS_ASSIGNED_USERS, is(Collections.emptyMap())))
                .andExpect(jsonPath(JSON_PATH_SKIPPED_ORGS).doesNotExist());

            // THEN (part 2)
            // ... verify no save, i.e. as no data to save
            verify(exactly(0), postRequestedFor(
                urlEqualTo(String.format(URL_SUPPLEMENTARY_UPDATE, CASE_ID_GOOD))
            ));
        }

        @DisplayName(
            "Should return 200 with `orgsAssignedUsers` with zeros if no users for any non-skipped organisation"
        )
        @Test
        void shouldReturn200_withOrgsAssignedUsersWithZeros_ifNoUsersForAnyNonSkippedOrg() throws Exception {

            // GIVEN
            stubCaseDetails(CASE_ID_GOOD, List.of(
                organisationPolicy(ORGANISATION_ID_1, ROLE_1),
                organisationPolicy(ORGANISATION_ID_BAD_1, ROLE_2) // to be skipped
            ));
            stubGetUsersByOrganisationInternal(usersByOrganisation(), ORGANISATION_ID_1); // i.e. no users for org
            stubGetUsersByOrganisationInternal(notFound(), ORGANISATION_ID_BAD_1); // i.e to be skipped

            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_CASE_ID, CASE_ID_GOOD);
            queryParams.add(PARAM_DRY_RUN_FLAG, "false");

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
                                .queryParams(queryParams)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN (part 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CASE_ID, is(CASE_ID_GOOD)))
                // .. verify `orgs_assigned_users`
                .andExpect(jsonPath(JSON_PATH_ORGS_ASSIGNED_USERS + "." + ORGANISATION_ID_1, is(0)))
                .andExpect(jsonPath(JSON_PATH_ORGS_ASSIGNED_USERS + "." + ORGANISATION_ID_BAD_1).doesNotExist())
                // .. verify `skipped_organisations`
                .andExpect(jsonPath(JSON_PATH_SKIPPED_ORGS + "." + ORGANISATION_ID_1).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_SKIPPED_ORGS + "." + ORGANISATION_ID_BAD_1).exists());

            // THEN (part 2)
            // ... verify no save, i.e. as no data to save
            verify(exactly(0), postRequestedFor(
                urlEqualTo(String.format(URL_SUPPLEMENTARY_UPDATE, CASE_ID_GOOD))
            ));
        }

        @DisplayName("Should return 200 with skipped organisation information if any user lookup errors")
        @Test
        void shouldReturn200_withSkippedOrgInformation_ifAnyUserLookupErrors() throws Exception {

            // GIVEN
            stubCaseDetails(CASE_ID_GOOD, List.of(
                organisationPolicy(ORGANISATION_ID_BAD_1, ROLE_1),
                organisationPolicy(ORGANISATION_ID_BAD_2, ROLE_2)
            ));
            stubGetUsersByOrganisationInternal(notFound(), ORGANISATION_ID_BAD_1);
            stubGetUsersByOrganisationInternal(serviceUnavailable(), ORGANISATION_ID_BAD_2);

            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_CASE_ID, CASE_ID_GOOD);
            queryParams.add(PARAM_DRY_RUN_FLAG, "true");

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
                                .queryParams(queryParams)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CASE_ID, is(CASE_ID_GOOD)))
                .andExpect(jsonPath(
                    JSON_PATH_SKIPPED_ORGS + "." + ORGANISATION_ID_BAD_1,
                    allOf(
                        containsString(ORGANISATION_ID_BAD_1),
                        containsString(HttpStatus.NOT_FOUND.getReasonPhrase())
                    )
                ))
                .andExpect(jsonPath(
                    JSON_PATH_SKIPPED_ORGS + "." + ORGANISATION_ID_BAD_2,
                    allOf(
                        containsString(ORGANISATION_ID_BAD_2),
                        containsString(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                    )
                ));
        }

        @DisplayName("Should return 200 with generated `orgsAssignedUsers` and no save as dry run")
        @Test
        void shouldReturn200_withOrgsAssignedUsersGenerated_andNoSaveAsDryRun() throws Exception {

            // GIVEN
            stubCaseDetails(CASE_ID_GOOD, List.of(
                organisationPolicy(ORGANISATION_ID_1, ROLE_1),
                organisationPolicy(ORGANISATION_ID_2, ROLE_2),
                organisationPolicy(null, ROLE_BAD),
                organisationPolicy(ORGANISATION_ID_1, ROLE_3) // NB: duplicate org
            ));
            stubGetUsersByOrganisationInternal(
                usersByOrganisation(user(USER_ID_1), user(USER_ID_2), user(USER_ID_BAD_1)),
                ORGANISATION_ID_1
            );
            stubGetUsersByOrganisationInternal(
                usersByOrganisation(user(USER_ID_1), user(USER_ID_BAD_2)), // i.e. user 1 both orgs
                ORGANISATION_ID_2
            );

            stubFindRoleAssignmentsByCasesAndUsers(
                List.of(CASE_ID_GOOD),
                List.of(USER_ID_1, USER_ID_2, USER_ID_BAD_1, USER_ID_BAD_2),
                roleAssignmentResponse(
                    // USER 1: in Org 1 & Org 2 and has org roles for both
                    roleAssignment(CASE_ID_GOOD, USER_ID_1, ROLE_1),
                    roleAssignment(CASE_ID_GOOD, USER_ID_1, ROLE_2),
                    roleAssignment(CASE_ID_GOOD, USER_ID_1, ROLE_3),
                    // USER 2: in Org 1 and has org roles
                    roleAssignment(CASE_ID_GOOD, USER_ID_2, ROLE_3),
                    // USER BAD 1: in org 1 but has a non-org role :: should be ignored
                    roleAssignment(CASE_ID_GOOD, USER_ID_BAD_1, ROLE_BAD),
                    // USER BAD 2: in org 2 but has an org1 role :: should be ignored
                    roleAssignment(CASE_ID_GOOD, USER_ID_BAD_2, ROLE_1)
                )
            );

            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_CASE_ID, CASE_ID_GOOD);
            queryParams.add(PARAM_DRY_RUN_FLAG, "true"); // i.e. is dry run

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
                                .queryParams(queryParams)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN (part 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CASE_ID, is(CASE_ID_GOOD)))
                .andExpect(jsonPath(
                    JSON_PATH_ORGS_ASSIGNED_USERS + "." + ORGANISATION_ID_1,
                    is(2) // ORG 1 = 2: 3 in org, but only 2 of those with an org role
                ))
                .andExpect(jsonPath(
                    JSON_PATH_ORGS_ASSIGNED_USERS + "." + ORGANISATION_ID_2,
                    is(1) // ORG 2 = 1: 2 in org, but only 1 of those with an org role
                ));

            // THEN (part 2)
            // ... verify no save, i.e. dry run
            verify(exactly(0), postRequestedFor(
                urlEqualTo(String.format(URL_SUPPLEMENTARY_UPDATE, CASE_ID_GOOD))
            ));

        }

        @DisplayName("Should return 200 with generated `orgsAssignedUsers` and save as not a dry run")
        @Test
        void shouldReturn200_withOrgsAssignedUsersGenerated_andSaveAsNotADryRun() throws Exception {

            // GIVEN
            stubCaseDetails(CASE_ID_GOOD, List.of(
                organisationPolicy(ORGANISATION_ID_1, ROLE_1),
                organisationPolicy(ORGANISATION_ID_2, ROLE_2),
                organisationPolicy(ORGANISATION_ID_1, ROLE_3) // NB: duplicate org
            ));
            stubGetUsersByOrganisationInternal(usersByOrganisation(user(USER_ID_1), user(USER_ID_2)),ORGANISATION_ID_1);
            stubGetUsersByOrganisationInternal(usersByOrganisation(user(USER_ID_1)), ORGANISATION_ID_2);

            stubFindRoleAssignmentsByCasesAndUsers(
                List.of(CASE_ID_GOOD),
                List.of(USER_ID_1, USER_ID_2),
                roleAssignmentResponse(
                    // USER 1: in Org 1 & Org 2 and has org roles for both
                    roleAssignment(CASE_ID_GOOD, USER_ID_1, ROLE_1),
                    roleAssignment(CASE_ID_GOOD, USER_ID_1, ROLE_2),
                    roleAssignment(CASE_ID_GOOD, USER_ID_1, ROLE_3),
                    // USER 2: in Org 1 and has org roles
                    roleAssignment(CASE_ID_GOOD, USER_ID_2, ROLE_3)
                )
            );

            stubUpdateSupplementaryData(CASE_ID_GOOD);

            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add(PARAM_CASE_ID, CASE_ID_GOOD);
            queryParams.add(PARAM_DRY_RUN_FLAG, "false"); // i.e not a dry run

            // expected count
            int org1Count = 2;
            int org2Count = 1;

            // expected save
            SupplementaryDataUpdateRequest expectedSaveRequest = generateSupplementaryDataUpdateRequest(Map.of(
                ORGANISATION_ID_1, org1Count,
                ORGANISATION_ID_2, org2Count
            ));

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_A_CASE_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
                                .queryParams(queryParams)
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN (part 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CASE_ID, is(CASE_ID_GOOD)))
                .andExpect(jsonPath(JSON_PATH_ORGS_ASSIGNED_USERS + "." + ORGANISATION_ID_1, is(org1Count)))
                .andExpect(jsonPath(JSON_PATH_ORGS_ASSIGNED_USERS + "." + ORGANISATION_ID_2, is(org2Count)));

            // THEN (part 2)
            // ... verify save attempt
            verify(exactly(1), postRequestedFor(
                urlEqualTo(String.format(URL_SUPPLEMENTARY_UPDATE, CASE_ID_GOOD))
            ).withRequestBody(
                equalToJson(
                    mapper.writeValueAsString(expectedSaveRequest),
                    true,
                    false
                )
            ));

        }

    }


    @Nested
    @DisplayName("POST " + ORGS_ASSIGNED_USERS_PATH + RESET_FOR_MULTIPLE_CASES_PATH)
    class RestOrganisationsAssignedUsersCountDataForMultipleCases {

        private static final String JSON_PATH_COUNT_DATA = "$.count_data";
        private static final String JSON_PATH_COUNT_DATA_BY_CASE_ID = JSON_PATH_COUNT_DATA + "[?(@.case_id=='%s')]";
        private static final String JSON_PATH_CASE_ID = JSON_PATH_COUNT_DATA_BY_CASE_ID + ".case_id";
        private static final String JSON_PATH_ERROR = JSON_PATH_COUNT_DATA_BY_CASE_ID + ".error";
        private static final String JSON_PATH_ORGS_ASSIGNED_USERS
            = JSON_PATH_COUNT_DATA_BY_CASE_ID + ".orgs_assigned_users";
        private static final String JSON_PATH_SKIPPED_ORGS
            = JSON_PATH_COUNT_DATA_BY_CASE_ID + ".skipped_organisations";

        @BeforeEach
        public void setUp() {
            // reset wiremock counters
            WireMock.resetAllRequests();

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
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
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
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
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
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
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
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
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
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
                                .content(mapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isBadRequest());
        }

        @DisplayName("Should return 200 with error information when case not found")
        @Test
        void shouldReturn200_withErrorInformation_whenCaseNotFound() throws Exception {

            // GIVEN
            stubGetCaseViaExternalApi(CASE_ID_NOT_FOUND, notFound());

            OrganisationsAssignedUsersResetRequest request = new OrganisationsAssignedUsersResetRequest(
                List.of(CASE_ID_NOT_FOUND),
                false
            );

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_MULTIPLE_CASES_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
                                .content(mapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN
                .andExpect(status().isOk())
                .andExpect(
                    jsonPath(String.format(JSON_PATH_CASE_ID, CASE_ID_NOT_FOUND)).value(CASE_ID_NOT_FOUND)
                )
                .andExpect(
                    jsonPath(String.format(JSON_PATH_ERROR, CASE_ID_NOT_FOUND),
                             hasItem(containsString(ValidationError.CASE_NOT_FOUND))
                    )
                );
        }

        @DisplayName("Should return 200 with multiple generated `orgsAssignedUsers` and no save as dry run")
        @Test
        void shouldReturn200_withMultipleOrgsAssignedUsersGenerated_andNoSaveAsDryRun() throws Exception {

            // GIVEN
            stubCaseDetails(CASE_ID_GOOD, List.of(
                organisationPolicy(ORGANISATION_ID_1, ROLE_1),
                organisationPolicy(ORGANISATION_ID_2, ROLE_2)
            ));
            stubCaseDetails(CASE_ID_GOOD_2, List.of(
                organisationPolicy(ORGANISATION_ID_3, ROLE_3)
            ));

            stubGetUsersByOrganisationInternal(usersByOrganisation(user(USER_ID_1)), ORGANISATION_ID_1);
            stubGetUsersByOrganisationInternal(usersByOrganisation(user(USER_ID_2)), ORGANISATION_ID_2);
            stubGetUsersByOrganisationInternal(
                usersByOrganisation(user(USER_ID_3), user(USER_ID_4)), ORGANISATION_ID_3
            );

            stubFindRoleAssignmentsByCasesAndUsers(
                List.of(CASE_ID_GOOD),
                List.of(USER_ID_1, USER_ID_2),
                roleAssignmentResponse(
                    roleAssignment(CASE_ID_GOOD, USER_ID_1, ROLE_1),
                    roleAssignment(CASE_ID_GOOD, USER_ID_2, ROLE_2)
                )
            );
            stubFindRoleAssignmentsByCasesAndUsers(
                List.of(CASE_ID_GOOD_2),
                List.of(USER_ID_3, USER_ID_4),
                roleAssignmentResponse(
                    roleAssignment(CASE_ID_GOOD_2, USER_ID_3, ROLE_3),
                    roleAssignment(CASE_ID_GOOD_2, USER_ID_4, ROLE_3)
                )
            );

            OrganisationsAssignedUsersResetRequest request = new OrganisationsAssignedUsersResetRequest(
                List.of(CASE_ID_GOOD, CASE_ID_GOOD_2),
                true // i.e. is dry run
            );

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_MULTIPLE_CASES_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
                                .content(mapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN (part 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_COUNT_DATA).exists())
                // ... verify case 1
                .andExpect(jsonPath(String.format(JSON_PATH_CASE_ID, CASE_ID_GOOD)).value(CASE_ID_GOOD))
                .andExpect(jsonPath(String.format(JSON_PATH_ORGS_ASSIGNED_USERS, CASE_ID_GOOD)).exists())
                .andExpect(
                    jsonPath(String.format(JSON_PATH_ORGS_ASSIGNED_USERS, CASE_ID_GOOD) + "." + ORGANISATION_ID_1)
                        .value(1)
                )
                .andExpect(
                    jsonPath(String.format(JSON_PATH_ORGS_ASSIGNED_USERS, CASE_ID_GOOD) + "." + ORGANISATION_ID_2)
                        .value(1)
                )
                // ... verify case 2
                .andExpect(jsonPath(String.format(JSON_PATH_CASE_ID, CASE_ID_GOOD_2)).value(CASE_ID_GOOD_2))
                .andExpect(jsonPath(String.format(JSON_PATH_ORGS_ASSIGNED_USERS, CASE_ID_GOOD_2)).exists())
                .andExpect(
                    jsonPath(String.format(JSON_PATH_ORGS_ASSIGNED_USERS, CASE_ID_GOOD_2) + "." + ORGANISATION_ID_3)
                        .value(2) // the only org with 2 users with the roles
                );

            // THEN (part 2)
            // ... verify no save, i.e. dry run
            verify(exactly(0), postRequestedFor(
                urlEqualTo(String.format(URL_SUPPLEMENTARY_UPDATE, CASE_ID_GOOD))
            ));
            verify(exactly(0), postRequestedFor(
                urlEqualTo(String.format(URL_SUPPLEMENTARY_UPDATE, CASE_ID_GOOD_2))
            ));
        }

        @DisplayName("Should return 200 with multiple generated `orgsAssignedUsers` and save as not a dry run")
        @Test
        void shouldReturn200_withMultipleOrgsAssignedUsersGenerated_andSaveAsNotADryRun() throws Exception {

            // GIVEN
            stubCaseDetails(CASE_ID_GOOD, List.of(
                organisationPolicy(ORGANISATION_ID_1, ROLE_1),
                organisationPolicy(ORGANISATION_ID_2, ROLE_2)
            ));
            stubCaseDetails(CASE_ID_GOOD_2, List.of(
                organisationPolicy(ORGANISATION_ID_1, ROLE_1),
                organisationPolicy(ORGANISATION_ID_2, ROLE_2) // NB: no user for case with role => should skip save
            ));
            stubCaseDetails(CASE_ID_GOOD_3, List.of(
                organisationPolicy(ORGANISATION_ID_1, ROLE_1),
                organisationPolicy(ORGANISATION_ID_BAD_1, ROLE_2),
                organisationPolicy(ORGANISATION_ID_BAD_2, ROLE_3),
                organisationPolicy(null, ROLE_BAD)
            ));
            stubGetCaseViaExternalApi(CASE_ID_NOT_FOUND, notFound());

            stubGetUsersByOrganisationInternal(
                usersByOrganisation(user(USER_ID_1), user(USER_ID_2)), ORGANISATION_ID_1
            );
            stubGetUsersByOrganisationInternal(
                usersByOrganisation(user(USER_ID_3), user(USER_ID_4)), ORGANISATION_ID_2
            );
            stubGetUsersByOrganisationInternal(notFound(), ORGANISATION_ID_BAD_1); // i.e to be skipped
            stubGetUsersByOrganisationInternal(serviceUnavailable(), ORGANISATION_ID_BAD_2); // i.e to be skipped

            stubFindRoleAssignmentsByCasesAndUsers(
                List.of(CASE_ID_GOOD),
                List.of(USER_ID_1, USER_ID_2, USER_ID_3, USER_ID_4),
                roleAssignmentResponse(
                    // Org 1: 2 users with role for org
                    roleAssignment(CASE_ID_GOOD, USER_ID_1, ROLE_1),
                    roleAssignment(CASE_ID_GOOD, USER_ID_2, ROLE_1),
                    // Org 2: ONLY 1 user with role for org
                    roleAssignment(CASE_ID_GOOD, USER_ID_3, ROLE_2)
                )
            );
            stubFindRoleAssignmentsByCasesAndUsers(
                List.of(CASE_ID_GOOD_2),
                List.of(USER_ID_1, USER_ID_2, USER_ID_3, USER_ID_4),
                roleAssignmentResponse(
                    // Org 1: ONLY 1 user with role for org
                    roleAssignment(CASE_ID_GOOD, USER_ID_1, ROLE_1)
                // NB: Org 2: no users with roles for org
                )
            );
            stubFindRoleAssignmentsByCasesAndUsers(
                List.of(CASE_ID_GOOD_3),
                List.of(USER_ID_1, USER_ID_2),
                roleAssignmentResponse(
                    // Org 1: 2 users with role for org
                    roleAssignment(CASE_ID_GOOD, USER_ID_1, ROLE_1),
                    roleAssignment(CASE_ID_GOOD, USER_ID_2, ROLE_1)
                )
            );

            stubUpdateSupplementaryData(CASE_ID_GOOD);
            stubUpdateSupplementaryData(CASE_ID_GOOD_2);
            stubUpdateSupplementaryData(CASE_ID_GOOD_3);

            OrganisationsAssignedUsersResetRequest request = new OrganisationsAssignedUsersResetRequest(
                List.of(CASE_ID_GOOD, CASE_ID_GOOD_2, CASE_ID_GOOD_3, CASE_ID_NOT_FOUND),
                false // i.e not a dry run
            );

            // expected count
            int case1Org1Count = 2;
            int case1Org2Count = 1;
            int case2Org1Count = 1;
            int case2Org2Count = 0; // NB: should be no save
            int case3Org1Count = 2;

            // expected save
            SupplementaryDataUpdateRequest expectedSaveRequestCase1 = generateSupplementaryDataUpdateRequest(Map.of(
                ORGANISATION_ID_1, case1Org1Count,
                ORGANISATION_ID_2, case1Org2Count
            ));
            SupplementaryDataUpdateRequest expectedSaveRequestCase2 = generateSupplementaryDataUpdateRequest(Map.of(
                ORGANISATION_ID_1, case2Org1Count // NB: no org2 as we don't save ZEROs
            ));
            //noinspection CheckStyle
            SupplementaryDataUpdateRequest expectedSaveRequestCase3 = generateSupplementaryDataUpdateRequest(Map.of(
                ORGANISATION_ID_1, case3Org1Count
            ));

            // WHEN
            mockMvc.perform(post(ORGS_ASSIGNED_USERS_PATH + RESET_FOR_MULTIPLE_CASES_PATH)
                                .header(SERVICE_AUTHORIZATION, S2S_TOKEN_GOOD)
                                .content(mapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
                // THEN (part 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_COUNT_DATA).exists())
                // ... verify case 1
                .andExpect(jsonPath(String.format(JSON_PATH_CASE_ID, CASE_ID_GOOD)).value(CASE_ID_GOOD))
                .andExpect(jsonPath(String.format(JSON_PATH_ORGS_ASSIGNED_USERS, CASE_ID_GOOD)).exists())
                .andExpect(
                    jsonPath(String.format(JSON_PATH_ORGS_ASSIGNED_USERS, CASE_ID_GOOD) + "." + ORGANISATION_ID_1)
                        .value(case1Org1Count)
                )
                .andExpect(
                    jsonPath(String.format(JSON_PATH_ORGS_ASSIGNED_USERS, CASE_ID_GOOD) + "." + ORGANISATION_ID_2)
                        .value(case1Org2Count)
                )
                // ... verify case 2
                .andExpect(jsonPath(String.format(JSON_PATH_CASE_ID, CASE_ID_GOOD_2)).value(CASE_ID_GOOD_2))
                .andExpect(jsonPath(String.format(JSON_PATH_ORGS_ASSIGNED_USERS, CASE_ID_GOOD_2)).exists())
                .andExpect(
                    jsonPath(String.format(JSON_PATH_ORGS_ASSIGNED_USERS, CASE_ID_GOOD_2) + "." + ORGANISATION_ID_1)
                        .value(case2Org1Count)
                )
                .andExpect(
                    jsonPath(String.format(JSON_PATH_ORGS_ASSIGNED_USERS, CASE_ID_GOOD_2) + "." + ORGANISATION_ID_2)
                        .value(case2Org2Count)
                )
                // ... verify case 3
                .andExpect(jsonPath(String.format(JSON_PATH_CASE_ID, CASE_ID_GOOD_3)).value(CASE_ID_GOOD_3))
                .andExpect(jsonPath(String.format(JSON_PATH_ORGS_ASSIGNED_USERS, CASE_ID_GOOD_3)).exists())
                .andExpect(
                    jsonPath(String.format(JSON_PATH_ORGS_ASSIGNED_USERS, CASE_ID_GOOD_3) + "." + ORGANISATION_ID_1)
                        .value(case3Org1Count)
                )
                .andExpect(
                    jsonPath(String.format(JSON_PATH_SKIPPED_ORGS + "." + ORGANISATION_ID_BAD_1, CASE_ID_GOOD_3))
                        .value(hasItem(
                            allOf(
                                containsString(ORGANISATION_ID_BAD_1),
                                containsString(HttpStatus.NOT_FOUND.getReasonPhrase())
                            )
                        ))
                )
                .andExpect(
                    jsonPath(String.format(JSON_PATH_SKIPPED_ORGS + "." + ORGANISATION_ID_BAD_2, CASE_ID_GOOD_3))
                        .value(hasItem(
                            allOf(
                                containsString(ORGANISATION_ID_BAD_2),
                                containsString(HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase())
                            )
                        ))
                )
                // ... verify case NOT FOUND
                .andExpect(jsonPath(String.format(JSON_PATH_CASE_ID, CASE_ID_NOT_FOUND)).value(CASE_ID_NOT_FOUND))
                .andExpect(jsonPath(String.format(JSON_PATH_ORGS_ASSIGNED_USERS, CASE_ID_NOT_FOUND)).doesNotExist())
                .andExpect(
                    jsonPath(String.format(JSON_PATH_ERROR, CASE_ID_NOT_FOUND))
                        .value(hasItem(containsString(ValidationError.CASE_NOT_FOUND)))
                );

            // THEN (part 2)
            // ... verify save attempt for case 1
            verify(exactly(1), postRequestedFor(
                urlEqualTo(String.format(URL_SUPPLEMENTARY_UPDATE, CASE_ID_GOOD))
            ).withRequestBody(
                equalToJson(
                    mapper.writeValueAsString(expectedSaveRequestCase1),
                    true,
                    false
                )
            ));
            // ... verify save attempt for case 2
            verify(exactly(1), postRequestedFor(
                urlEqualTo(String.format(URL_SUPPLEMENTARY_UPDATE, CASE_ID_GOOD_2))
            ).withRequestBody(
                equalToJson(
                    mapper.writeValueAsString(expectedSaveRequestCase2),
                    true,
                    false
                )
            ));
            // ... verify save attempt for case 3
            verify(exactly(1), postRequestedFor(
                urlEqualTo(String.format(URL_SUPPLEMENTARY_UPDATE, CASE_ID_GOOD_3))
            ).withRequestBody(
                equalToJson(
                    mapper.writeValueAsString(expectedSaveRequestCase3),
                    true,
                    false
                )
            ));
            // ... verify no save for case NOT FOUND
            verify(exactly(0), postRequestedFor(
                urlEqualTo(String.format(URL_SUPPLEMENTARY_UPDATE, CASE_ID_NOT_FOUND)
            )));
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

    private void stubCaseDetails(String caseId, List<OrganisationPolicy> orgPolicies) {
        Map<String, JsonNode> caseFields = new HashMap<>();

        for (int i = 0; i < orgPolicies.size(); i++) {
            caseFields.put("OrganisationPolicy" + i, mapper.convertValue(orgPolicies.get(i), JsonNode.class));
        }

        CaseDetails caseDetails = CaseDetails.builder()
            .id(caseId)
            .caseTypeId(CASE_TYPE_ID)
            .jurisdiction(JURISDICTION)
            .data(caseFields)
            .build();

        stubGetCaseViaExternalApi(caseId, caseDetails);
    }

    private SupplementaryDataUpdateRequest generateSupplementaryDataUpdateRequest(Map<String, Object> countsByOrg) {
        SupplementaryDataUpdateRequest updateRequest = new SupplementaryDataUpdateRequest();
        SupplementaryDataUpdates updates = new SupplementaryDataUpdates();
        updateRequest.setSupplementaryDataUpdates(updates);
        updates.setSetMap(countsByOrg.entrySet().stream()
                              .collect(Collectors.toMap(
                                  entry -> DefaultDataStoreRepository.ORGS_ASSIGNED_USERS_PATH + entry.getKey(),
                                  Map.Entry::getValue
                              ))
        );
        return updateRequest;
    }
}
