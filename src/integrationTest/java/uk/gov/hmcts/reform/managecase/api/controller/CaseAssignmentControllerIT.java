package uk.gov.hmcts.reform.managecase.api.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.StringUtils;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.managecase.BaseIT;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentRequest;
import uk.gov.hmcts.reform.managecase.api.payload.CaseUnassignmentRequest;
import uk.gov.hmcts.reform.managecase.api.payload.RequestedCaseUnassignment;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.TestFixtures;

import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.caseDetails;
import static uk.gov.hmcts.reform.managecase.TestFixtures.IdamFixture.userDetails;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.user;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.usersByOrganisation;
import static uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController.ASSIGN_ACCESS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController.CASE_ASSIGNMENTS_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController.GET_ASSIGNMENTS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController.UNASSIGN_ACCESS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient.CASE_USERS;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubAssignCase;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseAssignments;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseDetailsByCaseIdViaExternalApi;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetUsersByOrganisationExternal;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubIdamGetUserById;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubUnassignCase;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CASE_ROLE;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CASE_ROLE2;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ORGANIZATION_ID;

@SuppressWarnings({ "PMD.JUnitTestsShouldIncludeAssert", "PMD.MethodNamingConventions",
    "PMD.AvoidDuplicateLiterals", "PMD.ExcessiveImports", "PMD.TooManyMethods",
    "squid:S100", "squid:S1192" })
public class CaseAssignmentControllerIT {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String ASSIGNEE_ID = "ae2eb34c-816a-4eea-b714-6654d022fcef";
    private static final String ANOTHER_USER = "vcd345cvs-816a-4eea-b714-6654d022fcef";
    private static final String CASE_ID = "1588234985453946";
    private static final String CASE_ID2 = "1598630369818638";
    private static final String ORG_POLICY_ROLE = "[CaseRole1]";
    private static final String ORG_POLICY_ROLE2 = "[CaseRole2]";

    private static final List<String> NULL_CASE_ROLES = null;

    @Nested
    @DisplayName("POST /case-assignments")
    class AssignAccessWithinOrganisation extends BaseIT {

        private CaseAssignmentRequest request;

        @BeforeEach
        void setUp() {
            request = new CaseAssignmentRequest(CASE_TYPE_ID, CASE_ID, ASSIGNEE_ID);
            // Positive stub mappings - individual tests override again for a specific
            // scenario.
            stubGetUsersByOrganisationExternal(usersByOrganisation(user(ASSIGNEE_ID), user(ANOTHER_USER)));
            stubIdamGetUserById(ASSIGNEE_ID, userDetails(ASSIGNEE_ID,
                    "caseworker-AUTOTEST1-solicitor",
                    "caseworker-AUTOTEST1"));
            stubGetCaseDetailsByCaseIdViaExternalApi(CASE_ID, caseDetails(ORGANIZATION_ID, ORG_POLICY_ROLE));
            stubAssignCase(CASE_ID, ASSIGNEE_ID, ORG_POLICY_ROLE);
        }

        @DisplayName("Invoker successfully sharing case access with another solicitor in their org")
        @Test
        void shouldAssignCaseAccess_whenInvokerSuccessfullyShareACase() throws Exception {

            this.webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.status_message")
                        .isEqualTo(String.format(ASSIGN_ACCESS_MESSAGE, ORG_POLICY_ROLE));

            verify(postRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("Successfully sharing case access with multiple org roles")
        @Test
        void shouldAssignCaseAccess_withMultipleOrganisationRoles() throws Exception {

            stubGetCaseDetailsByCaseIdViaExternalApi(CASE_ID,
                    caseDetails(ORGANIZATION_ID, ORG_POLICY_ROLE, ORG_POLICY_ROLE2));
            stubAssignCase(CASE_ID, ASSIGNEE_ID, ORG_POLICY_ROLE, ORG_POLICY_ROLE2);

            this.webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.status_message")
                        .isEqualTo(String.format(ASSIGN_ACCESS_MESSAGE,
                                StringUtils.join(List.of(ORG_POLICY_ROLE, ORG_POLICY_ROLE2), ',')));

            verify(postRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("Invoker successfully sharing case access with another solicitor in their org"
                + " and jurisdiction role is mixed case")
        @Test
        void shouldAssignCaseAccess_whenInvokerSuccessfullyShareACaseWithMixedCaseJurisdictionRole()
                throws Exception {

            stubIdamGetUserById(ASSIGNEE_ID, userDetails(ASSIGNEE_ID,
                    "caseworker-AUTOTEST1-SoLiciToR",
                    "caseworker-AUTOTEST1"));

            this.webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.status_message")
                        .isEqualTo(String.format(ASSIGN_ACCESS_MESSAGE, ORG_POLICY_ROLE));

            verify(postRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("Invoker successfully sharing case access with another solicitor in their org"
                + " and jurisdiction role is upper case")
        @Test
        void shouldAssignCaseAccess_whenInvokerSuccessfullyShareACaseWithUpperCaseJurisdictionRole()
                throws Exception {

            stubIdamGetUserById(ASSIGNEE_ID, userDetails(ASSIGNEE_ID,
                    "CASEWORKER-AUTOTEST1-SOLICITOR",
                    "CASEWORKER-AUTOTEST1"));

            this.webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.status_message")
                        .isEqualTo(String.format(ASSIGN_ACCESS_MESSAGE, ORG_POLICY_ROLE));

            verify(postRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("Must return 400 bad request response if assignee doesn't exist in invoker's organisation")
        @Test
        void shouldReturn400_whenAssigneeNotExistsInInvokersOrg() throws Exception {

            stubGetUsersByOrganisationExternal(usersByOrganisation(user(ANOTHER_USER)));

            this.webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.message")
                        .isEqualTo(ValidationError.ASSIGNEE_ORGANISATION_ERROR);
        }

        @DisplayName("Must return 400 bad request response if assignee doesn't have a solicitor role for the"
                + " jurisdiction of the case")
        @Test
        void shouldReturn400_whenAssigneeNotHaveCorrectJurisdictionRole() throws Exception {

            stubGetUsersByOrganisationExternal(usersByOrganisation(user(ASSIGNEE_ID)));

            stubIdamGetUserById(ASSIGNEE_ID, userDetails(ASSIGNEE_ID, "caseworker-AUTOTEST2-solicitor-role"));

            this.webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.message")
                        .isEqualTo(ValidationError.ASSIGNEE_ROLE_ERROR);
        }

        @DisplayName("Must return 400 bad request response if assignee has an invalid solicitor role for the"
                + " jurisdiction of the case")
        @Test
        void shouldReturn400_whenAssigneeHasInvalidJurisdictionRole() throws Exception {

            stubGetUsersByOrganisationExternal(usersByOrganisation(user(ASSIGNEE_ID)));

            stubIdamGetUserById(ASSIGNEE_ID, userDetails(ASSIGNEE_ID, "caseworker-AUTOTEST2-solicit"));

            this.webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.message")
                        .isEqualTo(ValidationError.ASSIGNEE_ROLE_ERROR);
        }

        @DisplayName("Must return 400 bad request response if invoker's organisation is not present"
                + " in the case data organisation policies")
        @Test
        void shouldReturn400_whenInvokersOrgIsNotPresentInCaseData() throws Exception {
            stubGetCaseDetailsByCaseIdViaExternalApi(CASE_ID, caseDetails("ANOTHER_ORGANIZATION_ID", ORG_POLICY_ROLE));

            this.webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.message")
                        .isEqualTo(ValidationError.ORGANISATION_POLICY_ERROR);
        }

        @DisplayName("Must return 404 server error response if case could not be found")
        @Test
        void shouldReturn404_whenCaseNotFound() throws Exception {
            // ARRANGE
            stubGetCaseDetailsByCaseIdViaExternalApi(CASE_ID, null); // i.e. no case not found

            // ACT + ASSERT
            this.webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.message")
                        .isEqualTo(ValidationError.CASE_NOT_FOUND);

            // ASSERT
            verify(exactly(0), deleteRequestedFor(urlEqualTo(CASE_USERS)));
        }
    }

    @Nested
    @DisplayName("GET /case-assignments")
    class GetCaseAssignments extends BaseIT {

        @DisplayName("Successfully return case assignments of my organisation")
        @Test
        void shouldGetCaseAssignments_forAValidRequest() throws Exception {

            CaseUserRole caseUserRole = CaseUserRole.builder()
                    .caseId(CASE_ID)
                    .userId(ASSIGNEE_ID)
                    .caseRole(CASE_ROLE)
                    .build();

            stubGetUsersByOrganisationExternal(usersByOrganisation(user(ASSIGNEE_ID)));

            stubGetCaseAssignments(List.of(CASE_ID), List.of(ASSIGNEE_ID), List.of(caseUserRole));

            this.webClient.get()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .attribute("case_ids", CASE_ID)
                .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.status_message")
                        .isEqualTo(GET_ASSIGNMENTS_MESSAGE)
                    .jsonPath("$.case_assignments.length()")
                        .isEqualTo(1)
                    .jsonPath("$.case_assignments[0].case_id")
                        .isEqualTo(CASE_ID)
                    .jsonPath("$.case_assignments[0].shared_with.length()")
                        .isEqualTo(1)
                    .jsonPath("$.case_assignments[0].shared_with[0].first_name")
                        .isEqualTo(TestFixtures.FIRST_NAME)
                    .jsonPath("$.case_assignments[0].shared_with[0].last_name")
                        .isEqualTo(TestFixtures.LAST_NAME)
                    .jsonPath("$.case_assignments[0].shared_with[0].email")
                        .isEqualTo(TestFixtures.EMAIL)
                    .jsonPath("$.case_assignments[0].shared_with[0].case_roles.length()")
                        .isEqualTo(1)
                    .jsonPath("$.case_assignments[0].shared_with[0].case_roles[0]")
                        .isEqualTo(CASE_ROLE);
        }

        @DisplayName("Must return 400 bad request response if caseIds are missing in GetAssignments request")
        @Test
        void shouldReturn400_whenCaseIdsAreNotPassedForGetAssignmentsApi() throws Exception {

            this.webClient.get()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .attribute("case_ids", "")
                .exchange()
                    .expectStatus().isBadRequest();
        }

    }

    @Nested
    @DisplayName("DELETE /case-assignments")
    class UnassignAccessWithinOrganisation extends BaseIT {

        @BeforeEach
        void setUp() {
            // reset wiremock counters
            WireMock.resetAllRequests();

            // Positive stub mappings - individual tests override again for a specific
            // scenario.
            stubGetUsersByOrganisationExternal(usersByOrganisation(user(ASSIGNEE_ID), user(ANOTHER_USER)));
        }

        @DisplayName("Should unassign case successfully for a valid request: single case-role")
        @Test
        void shouldUnassignCaseAccess_singleCaseRole() throws Exception {
            // ARRANGE
            // :: request data
            List<RequestedCaseUnassignment> unassignments = List.of(
                    new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of(CASE_ROLE)));
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // :: data store stub data
            List<CaseUserRoleWithOrganisation> expectedUnassignments = List.of(
                    new CaseUserRoleWithOrganisation(CASE_ID, ASSIGNEE_ID, CASE_ROLE, ORGANIZATION_ID));
            stubUnassignCase(expectedUnassignments);

            // ACT + ASSERT
            this.webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.status_message")
                        .isEqualTo(UNASSIGN_ACCESS_MESSAGE);

            // ASSERT
            verify(exactly(1), deleteRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("Should unassign case successfully for a valid request: multiple case-roles")
        @Test
        void shouldUnassignCaseAccess_multipleCaseRoles() throws Exception {
            // ARRANGE
            // :: request data
            List<RequestedCaseUnassignment> unassignments = List.of(
                    new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of(CASE_ROLE)),
                    new RequestedCaseUnassignment(CASE_ID2, ANOTHER_USER, List.of(CASE_ROLE, CASE_ROLE2)));
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // :: data store stub data
            List<CaseUserRoleWithOrganisation> expectedUnassignments = List.of(
                    new CaseUserRoleWithOrganisation(CASE_ID, ASSIGNEE_ID, CASE_ROLE, ORGANIZATION_ID),
                    new CaseUserRoleWithOrganisation(CASE_ID2, ANOTHER_USER, CASE_ROLE, ORGANIZATION_ID),
                    new CaseUserRoleWithOrganisation(CASE_ID2, ANOTHER_USER, CASE_ROLE2, ORGANIZATION_ID));
            stubUnassignCase(expectedUnassignments);

            // ACT + ASSERT
            this.webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.status_message")
                        .isEqualTo(UNASSIGN_ACCESS_MESSAGE);

            // ASSERT
            verify(exactly(1), deleteRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("Should unassign case successfully after looking up role information when not supplied")
        @Test
        void shouldUnassignCaseAccess_afterLookingUpCaseRoles() throws Exception {
            // ARRANGE
            // :: request data
            List<RequestedCaseUnassignment> unassignments = List.of(
                    new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, NULL_CASE_ROLES),
                    new RequestedCaseUnassignment(CASE_ID2, ANOTHER_USER, NULL_CASE_ROLES));
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // :: data store stub data
            // :: :: getCaseAssignments
            stubGetCaseAssignments(List.of(CASE_ID, CASE_ID2),
                    List.of(ASSIGNEE_ID, ANOTHER_USER),
                    List.of(
                            new CaseUserRole(CASE_ID, ASSIGNEE_ID, CASE_ROLE),
                            new CaseUserRole(CASE_ID, ASSIGNEE_ID, CASE_ROLE2),
                            // extra return item from get call not in unassignment request ...
                            // ... therefore must be ignored (i.e. not in expectedUnassignments)
                            new CaseUserRole(CASE_ID2, ASSIGNEE_ID, CASE_ROLE) // NB: extra
                    ));
            // :: :: unassignCase
            List<CaseUserRoleWithOrganisation> expectedUnassignments = List.of(
                    new CaseUserRoleWithOrganisation(CASE_ID, ASSIGNEE_ID, CASE_ROLE, ORGANIZATION_ID),
                    new CaseUserRoleWithOrganisation(CASE_ID, ASSIGNEE_ID, CASE_ROLE2, ORGANIZATION_ID)
            // NB: extra CaseUserRole from above not defined in expected list as not part of
            // unassignment request
            );
            stubUnassignCase(expectedUnassignments);

            // ACT + ASSERT
            this.webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.status_message")
                        .isEqualTo(UNASSIGN_ACCESS_MESSAGE);

            // ASSERT
            verify(exactly(1), deleteRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("Should NOT unassign case if looking up of role information returned empty")
        @Test
        void shouldNotUnassignCaseAccess_afterLookingUpCaseRolesReturnedEmpty() throws Exception {
            // ARRANGE
            // :: request data
            List<RequestedCaseUnassignment> unassignments = List.of(
                    new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, NULL_CASE_ROLES),
                    new RequestedCaseUnassignment(CASE_ID2, ANOTHER_USER, NULL_CASE_ROLES));
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // :: data store stub data
            // :: :: getCaseAssignments
            stubGetCaseAssignments(List.of(CASE_ID, CASE_ID2),
                    List.of(ASSIGNEE_ID, ANOTHER_USER),
                    List.of()); // i.e. empty result

            // :: NB: no stubbing for unassignCase as call not expected

            // ACT + ASSERT
            this.webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.status_message")
                        .isEqualTo(UNASSIGN_ACCESS_MESSAGE);

            // ASSERT
            verify(exactly(0), deleteRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("should fail with 400 bad request when assignee is not found in the organisation")
        @Test
        void shouldReturn400_whenAssigneeNotInOrganisation() throws Exception {
            // ARRANGE
            // :: request data
            List<RequestedCaseUnassignment> unassignments = List.of(
                    new RequestedCaseUnassignment(CASE_ID, "ASSIGNEE-NOT-IN-ORG", NULL_CASE_ROLES));
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // :: data store stub data
            // :: NB: no stubbing for unassignCase as call not expected

            // ACT + ASSERT
            this.webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.message")
                        .isEqualTo(ValidationError.UNASSIGNEE_ORGANISATION_ERROR);

            // ASSERT
            verify(exactly(0), deleteRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("should fail with 400 bad request when unassignments list is null")
        @Test
        void shouldReturn400_whenUnassignmentsListIsNull() throws Exception {
            // ARRANGE
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(null);

            // ACT + ASSERT
            this.webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors.length()")
                        .isEqualTo(1)
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.EMPTY_REQUESTED_UNASSIGNMENTS_LIST);

            // ASSERT
            verify(exactly(0), deleteRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("should fail with 400 bad request when unassignments list is empty")
        @Test
        void shouldReturn400_whenUnassignmentsListIsEmpty() throws Exception {
            // ARRANGE
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(List.of());

            // ACT + ASSERT
            this.webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors.length()")
                        .isEqualTo(1)
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.EMPTY_REQUESTED_UNASSIGNMENTS_LIST);

            // ASSERT
            verify(exactly(0), deleteRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("should fail with 400 bad request when case id is null")
        @Test
        void shouldReturn400_whenCaseIdIsNull() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                    new RequestedCaseUnassignment(null, ASSIGNEE_ID, Collections.emptyList()));
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // ACT + ASSERT
            this.webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors.length()")
                        .isEqualTo(1)
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.CASE_ID_EMPTY);

            // ASSERT
            verify(exactly(0), deleteRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("should fail with 400 bad request when case id is invalid")
        @Test
        void shouldReturn400_whenCaseIdIsInvalid() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                    new RequestedCaseUnassignment("invalid", ASSIGNEE_ID, List.of()));
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // ACT + ASSERT
            this.webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors.length()")
                        .isEqualTo(2)
                    .jsonPath("$.errors")
                        .value(Matchers.containsInAnyOrder(
                            ValidationError.CASE_ID_INVALID_LENGTH,
                            ValidationError.CASE_ID_INVALID
                        ));

            // ASSERT
            verify(exactly(0), deleteRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("should fail with 400 bad request when assignee id is null")
        @Test
        void shouldReturn400_whenAssigneeIdIsNull() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                    new RequestedCaseUnassignment(CASE_ID, null, Collections.emptyList()));
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // ACT + ASSERT
            this.webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors.length()")
                        .isEqualTo(1)
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.ASSIGNEE_ID_EMPTY);

            // ASSERT
            verify(exactly(0), deleteRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("should fail with 400 bad request when assignee id is empty")
        @Test
        void shouldReturn400_whenAssigneeIdIsEmpty() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                    new RequestedCaseUnassignment(CASE_ID, "", List.of()));
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // ACT + ASSERT
            this.webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors.length()")
                        .isEqualTo(1)
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.ASSIGNEE_ID_EMPTY);

            // ASSERT
            verify(exactly(0), deleteRequestedFor(urlEqualTo(CASE_USERS)));
        }

        @DisplayName("should fail with 400 bad request when a case role is invalid")
        @Test
        void shouldReturn400_whenAACaseRoleIsInvalid() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                    new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of(CASE_ROLE, "INVALID")));
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // ACT + ASSERT
            this.webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors.length()")
                        .isEqualTo(1)
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.CASE_ROLE_FORMAT_INVALID);

            // ASSERT
            verify(exactly(0), deleteRequestedFor(urlEqualTo(CASE_USERS)));
        }

    }

}
