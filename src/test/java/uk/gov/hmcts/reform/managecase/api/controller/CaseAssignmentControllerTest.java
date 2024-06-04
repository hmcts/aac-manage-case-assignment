package uk.gov.hmcts.reform.managecase.api.controller;

import feign.FeignException;
import feign.Request;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.modelmapper.ModelMapper;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import uk.gov.hmcts.reform.managecase.BaseTest;
import uk.gov.hmcts.reform.managecase.TestFixtures;
import uk.gov.hmcts.reform.managecase.TestFixtures.CaseAssignedUsersFixture;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentRequest;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignmentResponse;
import uk.gov.hmcts.reform.managecase.api.payload.CaseUnassignmentRequest;
import uk.gov.hmcts.reform.managecase.api.payload.CaseUnassignmentResponse;
import uk.gov.hmcts.reform.managecase.api.payload.GetCaseAssignmentsResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RequestedCaseUnassignment;
import uk.gov.hmcts.reform.managecase.config.MapperConfig;
import uk.gov.hmcts.reform.managecase.config.SecurityConfiguration;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignedUsers;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.domain.UserDetails;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;
import uk.gov.hmcts.reform.managecase.service.common.UIDService;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController.CASE_ASSIGNMENTS_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController.GET_ASSIGNMENTS_MESSAGE;


@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert", "PMD.ExcessiveImports",
    "squid:S2699"})
@WebFluxTest(controllers = CaseAssignmentController.class,
    includeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, 
        classes = { MapperConfig.class }
    ),
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE, 
        classes = { SecurityConfiguration.class, JwtGrantedAuthoritiesConverter.class }
    )
)
public class CaseAssignmentControllerTest extends BaseTest {

    private static final String ASSIGNEE_ID = "0a5874a4-3f38-4bbd-ba4c";
    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String CASE_ID = "1588234985453946";

    @MockBean
    protected CaseAssignmentService service;

    @MockBean
    protected UIDService uidService;

    @MockBean
    protected IdamRepository idamRepository;

    @MockBean
    protected SecurityUtils securityUtils;

    @Nested
    @DisplayName("POST /case-assignments")
    class AssignAccessWithinOrganisation {
        private static final String USE_USER_TOKEN_PARAM = "use_user_token=true";
        private CaseAssignmentRequest request;

        @BeforeEach
        void setUp() {
            request = new CaseAssignmentRequest(CASE_TYPE_ID, CASE_ID, ASSIGNEE_ID);
        }

        @DisplayName("happy path test without mockWebFlux")
        @Test
        void directCallHappyPath() { // created to avoid IDE warnings in controller class that function is never used
            // ARRANGE
            List<String> roles = List.of("Role1", "Role2");
            given(service.assignCaseAccess(any(CaseAssignment.class), anyBoolean())).willReturn(roles);

            CaseAssignmentController controller = new CaseAssignmentController(service, new ModelMapper());

            // ACT
            CaseAssignmentResponse response = controller.assignAccessWithinOrganisation(request, Optional.of(false));

            // ASSERT
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(
                    "Roles Role1,Role2 from the organisation policies successfully assigned to the assignee.");
        }

        @DisplayName("should assign case successfully for a valid request")
        @Test
        void shouldAssignCaseAccess() throws Exception {
            List<String> roles = List.of("Role1", "Role2");
            given(service.assignCaseAccess(any(CaseAssignment.class), anyBoolean())).willReturn(roles);

            webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isCreated()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.status_message")
                        .isEqualTo("Roles Role1,Role2 from the organisation"
                            + " policies successfully assigned to the assignee.");
        }

        @DisplayName("should delegate to service domain for a valid request")
        @Test
        void shouldDelegateToServiceDomain() throws Exception {
            webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isCreated();

            ArgumentCaptor<CaseAssignment> captor = ArgumentCaptor.forClass(CaseAssignment.class);
            verify(service).assignCaseAccess(captor.capture(), eq(false));
            assertThat(captor.getValue().getCaseId()).isEqualTo(CASE_ID);
            assertThat(captor.getValue().getAssigneeId()).isEqualTo(ASSIGNEE_ID);
        }

        @DisplayName("should delegate to service domain for a valid request with provided use_user_token request param")
        @Test
        void shouldDelegateToServiceDomainWithProvidedUseUserTokenParam() throws Exception {
            webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH + "?" + USE_USER_TOKEN_PARAM)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isCreated();

            ArgumentCaptor<CaseAssignment> captor = ArgumentCaptor.forClass(CaseAssignment.class);
            verify(service).assignCaseAccess(captor.capture(), eq(true));
            assertThat(captor.getValue().getCaseId()).isEqualTo(CASE_ID);
            assertThat(captor.getValue().getAssigneeId()).isEqualTo(ASSIGNEE_ID);
        }

        @DisplayName("should fail with 400 bad request when case type id is null")
        @Test
        void shouldFailWithBadRequestWhenCaseTypeIdIsNull() throws Exception {
            request = new CaseAssignmentRequest(null, CASE_ID, ASSIGNEE_ID);

            webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.CASE_TYPE_ID_EMPTY);
        }

        @DisplayName("should fail with 400 bad request when case id is null")
        @Test
        void shouldFailWithBadRequestWhenCaseIdIsNull() throws Exception {
            request = new CaseAssignmentRequest(CASE_TYPE_ID, null, ASSIGNEE_ID);

            webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.CASE_ID_EMPTY);
        }

        @DisplayName("should fail with 400 bad request when case id is an invalid Luhn number")
        @Test
        void shouldFailWithBadRequestWhenCaseIdIsInvalidLuhnNumber() throws Exception {
            request = new CaseAssignmentRequest(CASE_TYPE_ID, "123", ASSIGNEE_ID);

            webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors.length()")
                        .isEqualTo(2)
                    .jsonPath("$.errors[*]")
                        .value(Matchers.containsInAnyOrder(
                            ValidationError.CASE_ID_INVALID_LENGTH, 
                            ValidationError.CASE_ID_INVALID
                        ))   
                ;
        }

        @DisplayName("should fail with 400 bad request when assignee id is empty")
        @Test
        void shouldFailWithBadRequestWhenAssigneeIdIsNull() throws Exception {
            request = new CaseAssignmentRequest(CASE_TYPE_ID, CASE_ID, "");

            webClient.post()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.ASSIGNEE_ID_EMPTY);
        }
    }

    @Nested
    @DisplayName("GET /case-assignments")
    class GetCaseAssignments {

        @DisplayName("happy path test without mockWebFlux")
        @Test
        void directCallHappyPath() { // created to avoid IDE warnings in controller class that function is never used
            // ARRANGE
            List<String> caseIds = List.of("1588234985453946", "1588234985453948");
            List<CaseAssignedUsers> caseAssignedUsers = List.of(CaseAssignedUsersFixture.caseAssignedUsers());

            given(service.getCaseAssignments(caseIds)).willReturn(caseAssignedUsers);

            CaseAssignmentController controller = new CaseAssignmentController(service, new ModelMapper());

            // ACT
            GetCaseAssignmentsResponse response = controller.getCaseAssignments(caseIds);

            // ASSERT
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(GET_ASSIGNMENTS_MESSAGE);
            assertThat(response.getCaseAssignedUsers()).hasSize(1);
            assertThat(response.getCaseAssignedUsers().get(0).getCaseId()).isEqualTo(TestFixtures.CASE_ID);
            assertThat(response.getCaseAssignedUsers().get(0).getUsers()).hasSize(1);
            UserDetails firstUserDetails = response.getCaseAssignedUsers().get(0).getUsers().get(0);
            assertThat(firstUserDetails.getFirstName()).isEqualTo(TestFixtures.FIRST_NAME);
            assertThat(firstUserDetails.getLastName()).isEqualTo(TestFixtures.LAST_NAME);
            assertThat(firstUserDetails.getEmail()).isEqualTo(TestFixtures.EMAIL);
            assertThat(firstUserDetails.getCaseRoles())
                .hasSize(2)
                .contains(TestFixtures.CASE_ROLE)
                .contains(TestFixtures.CASE_ROLE2);
        }

        @DisplayName("should successfully get case assignments")
        @Test
        void shouldGetCaseAssignmentsForAValidRequest() throws Exception {
            String caseIds = "1588234985453946,1588234985453948";
            List<CaseAssignedUsers> caseAssignedUsers = List.of(CaseAssignedUsersFixture.caseAssignedUsers());

            given(service.getCaseAssignments(List.of(caseIds.split(",")))).willReturn(caseAssignedUsers);

            webClient.get()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .attribute("case_ids", caseIds)
                .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(APPLICATION_JSON_VALUE)
                .expectBody()
                    .jsonPath("$.status_message")
                        .isEqualTo(GET_ASSIGNMENTS_MESSAGE)
                    .jsonPath("$.case_assignments.length()")
                        .isEqualTo(1)
                    .jsonPath("$.case_assignments[0].case_id")
                        .isEqualTo(TestFixtures.CASE_ID)
                    .jsonPath("$.case_assignments[0].shared_with.length()")
                        .isEqualTo(1)
                    .jsonPath("$.case_assignments[0].shared_with[0].first_name")
                        .isEqualTo(TestFixtures.FIRST_NAME)
                    .jsonPath("$.case_assignments[0].shared_with[0].last_name")
                        .isEqualTo(TestFixtures.LAST_NAME)
                    .jsonPath("$.case_assignments[0].shared_with[0].email")
                        .isEqualTo(TestFixtures.EMAIL)
                    .jsonPath("$.case_assignments[0].shared_with[0].case_roles.length()")
                        .isEqualTo(2)
                    .jsonPath("$.case_assignments[0].shared_with[0].case_roles[0]")
                        .isEqualTo(TestFixtures.CASE_ROLE)
                    .jsonPath("$.case_assignments[0].shared_with[0].case_roles[1]")
                        .isEqualTo(TestFixtures.CASE_ROLE2)
                    ;
        }

        @DisplayName("should return 500 when downstream throws FeignException for get case assignments")
        @Test
        void shouldReturn500ErrorWhenDownstreamCallFailed() throws Exception {
            String caseIds = "1588234985453946";
            Request request = Request.create(Request.HttpMethod.GET, "someUrl", Map.of(), null,
                                             Charset.defaultCharset(), null
            );
            given(service.getCaseAssignments(List.of(caseIds)))
                .willThrow(new FeignException.NotFound("404", request, "data store failure".getBytes(),
                                                       new HashMap<String, Collection<String>>()));

            webClient.get()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .attribute("case_ids", caseIds)
                .exchange()
                    .expectStatus().is5xxServerError()
                .expectBody()
                    .jsonPath("$.message")
                        .value(Matchers.containsString("data store failure"));

        }

        @DisplayName("should fail with 400 bad request when caseIds query param is not passed")
        @Test
        void shouldFailWithBadRequestWhenCaseIdsInGetAssignmentsIsNull() throws Exception {

            webClient.get()
                    .uri(CASE_ASSIGNMENTS_PATH)
                .exchange()
                    .expectStatus().isBadRequest();
        }

        @DisplayName("should fail with 400 bad request when caseIds is empty")
        @Test
        void shouldFailWithBadRequestWhenCaseIdsInGetAssignmentsIsEmpty() throws Exception {

            webClient.get()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .attribute("case_ids", "")
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.message")
                        .value(Matchers.containsString("case_ids must be a non-empty list of proper case ids."));
        }

        @DisplayName("should fail with 400 bad request when caseIds is malformed or invalid")
        @Test
        void shouldFailWithBadRequestWhenCaseIdsInGetAssignmentsIsMalformed() throws Exception {

            webClient.get()
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .attribute("case_ids", "121324,%12345")
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.message")
                        .isEqualTo("Case ID should contain digits only");
        }
    }

    @Nested
    @DisplayName("DELETE /case-assignments")
    class UnassignAccessWithinOrganisation {

        @Captor
        private ArgumentCaptor<ArrayList<RequestedCaseUnassignment>> requestedCaseUnassignmentsCaptor;

        @DisplayName("happy path test without mockWebFlux")
        @Test
        void directCallHappyPath() { // created to avoid IDE warnings in controller class that function is never used
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, null),
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of("[CR1]", "[CR2]"))
            );
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            CaseAssignmentController controller = new CaseAssignmentController(service, new ModelMapper());

            // ACT
            CaseUnassignmentResponse response = controller.unassignAccessWithinOrganisation(request);

            // ASSERT
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(CaseAssignmentController.UNASSIGN_ACCESS_MESSAGE);
        }

        @DisplayName("should unassign case successfully for a valid request")
        @Test
        void shouldUnassignCaseAccess() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, null),
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of("[CR1]", "[CR2]"))
            );
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // ACT + ASSERT
            webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                    .jsonPath("$.status_message")
                        .isEqualTo(CaseAssignmentController.UNASSIGN_ACCESS_MESSAGE)
                ;
        }

        @DisplayName("should delegate to service domain for a valid request")
        @Test
        void shouldDelegateToServiceDomain() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of()),
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of("[CR1]", "[CR2]"))
            );
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // ACT + ASSERT
            webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isOk();

            // ASSERT
            verify(service).unassignCaseAccess(requestedCaseUnassignmentsCaptor.capture());
            List<RequestedCaseUnassignment> captorValue = requestedCaseUnassignmentsCaptor.getValue();
            assertThat(captorValue)
                .hasSameSizeAs(unassignments)
                .containsAll(unassignments);
        }

        @DisplayName("should fail with 400 bad request when unassignments list is null")
        @Test
        void shouldFailWithBadRequestWhenUnassignmentsListIsNull() throws Exception {
            // ARRANGE
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(null);

            webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.EMPTY_REQUESTED_UNASSIGNMENTS_LIST);

        }

        @DisplayName("should fail with 400 bad request when unassignments list is empty")
        @Test
        void shouldFailWithBadRequestWhenUnassignmentsListIsEmpty() throws Exception {
            // ARRANGE
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(List.of());

            webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.EMPTY_REQUESTED_UNASSIGNMENTS_LIST);

        }

        @DisplayName("should fail with 400 bad request when case id is null")
        @Test
        void shouldFailWithBadRequestWhenCaseIdIsNull() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                new RequestedCaseUnassignment(null, ASSIGNEE_ID, List.of())
            );
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // ACT + ASSERT
            webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.CASE_ID_EMPTY);
        }

        @DisplayName("should fail with 400 bad request when case id is invalid")
        @Test
        void shouldFailWithBadRequestWhenCaseIdIsInvalid() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                new RequestedCaseUnassignment("invalid", ASSIGNEE_ID, List.of())
            );
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors.length()")
                        .isEqualTo(2)
                    .jsonPath("$.errors[*]")
                        .value(Matchers.containsInAnyOrder(
                            ValidationError.CASE_ID_INVALID_LENGTH, 
                            ValidationError.CASE_ID_INVALID
                        ));
        }

        @DisplayName("should fail with 400 bad request when assignee id is null")
        @Test
        void shouldFailWithBadRequestWhenAssigneeIdIsNull() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                new RequestedCaseUnassignment(CASE_ID, null, List.of())
            );
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // ACT + ASSERT
            webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.ASSIGNEE_ID_EMPTY);
        }

        @DisplayName("should fail with 400 bad request when assignee id is empty")
        @Test
        void shouldFailWithBadRequestWhenAssigneeIdIsEmpty() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                new RequestedCaseUnassignment(CASE_ID, "", List.of())
            );
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.ASSIGNEE_ID_EMPTY);
        }

        @DisplayName("should fail with 400 bad request when a case role is invalid")
        @Test
        void shouldFailWithBadRequestWhenACaseRoleIsInvalid() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of("[CR1]", "INVALID"))
            );
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            webClient.method(HttpMethod.DELETE)
                    .uri(CASE_ASSIGNMENTS_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                    .expectStatus().isBadRequest()
                .expectBody()
                    .jsonPath("$.errors[0]")
                        .isEqualTo(ValidationError.CASE_ROLE_FORMAT_INVALID);
        }
    }
}
