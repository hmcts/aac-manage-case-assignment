package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.managecase.TestFixtures;
import uk.gov.hmcts.reform.managecase.TestFixtures.CaseAssignedUsersFixture;
import uk.gov.hmcts.reform.managecase.TestIdamConfiguration;
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
import uk.gov.hmcts.reform.managecase.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController.CASE_ASSIGNMENTS_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController.GET_ASSIGNMENTS_MESSAGE;


@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert", "PMD.ExcessiveImports",
    "squid:S2699"})
public class CaseAssignmentControllerTest {

    private static final String ASSIGNEE_ID = "0a5874a4-3f38-4bbd-ba4c";
    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String CASE_ID = "1588234985453946";

    @WebMvcTest(controllers = CaseAssignmentController.class,
        includeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = MapperConfig.class),
        excludeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes =
            { SecurityConfiguration.class, JwtGrantedAuthoritiesConverter.class }))
    @AutoConfigureMockMvc(addFilters = false)
    @ImportAutoConfiguration(TestIdamConfiguration.class)
    static class BaseMvcTest {

        @Autowired
        protected MockMvc mockMvc;

        @MockBean
        protected CaseAssignmentService service;

        @Autowired
        protected ObjectMapper objectMapper;
    }

    @Nested
    @DisplayName("POST /case-assignments")
    @SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert", "PMD.ExcessiveImports"})
    class AssignAccessWithinOrganisation extends BaseMvcTest {

        private CaseAssignmentRequest request;

        @BeforeEach
        void setUp() {
            request = new CaseAssignmentRequest(CASE_TYPE_ID, CASE_ID, ASSIGNEE_ID);
        }

        @DisplayName("happy path test without mockMvc")
        @Test
        void directCallHappyPath() { // created to avoid IDE warnings in controller class that function is never used
            // ARRANGE
            List<String> roles = List.of("Role1", "Role2");
            given(service.assignCaseAccess(any(CaseAssignment.class))).willReturn(roles);

            CaseAssignmentController controller = new CaseAssignmentController(service, new ModelMapper());

            // ACT
            CaseAssignmentResponse response = controller.assignAccessWithinOrganisation(request);

            // ASSERT
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(
                    "Roles Role1,Role2 from the organisation policies successfully assigned to the assignee.");
        }

        @DisplayName("should assign case successfully for a valid request")
        @Test
        void shouldAssignCaseAccess() throws Exception {
            List<String> roles = List.of("Role1", "Role2");
            given(service.assignCaseAccess(any(CaseAssignment.class))).willReturn(roles);

            this.mockMvc.perform(post(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status_message", is(
                    "Roles Role1,Role2 from the organisation policies successfully assigned to the assignee.")));
        }

        @DisplayName("should delegate to service domain for a valid request")
        @Test
        void shouldDelegateToServiceDomain() throws Exception {
            this.mockMvc.perform(post(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

            ArgumentCaptor<CaseAssignment> captor = ArgumentCaptor.forClass(CaseAssignment.class);
            verify(service).assignCaseAccess(captor.capture());
            assertThat(captor.getValue().getCaseId()).isEqualTo(CASE_ID);
            assertThat(captor.getValue().getAssigneeId()).isEqualTo(ASSIGNEE_ID);
        }

        @DisplayName("should fail with 400 bad request when case type id is null")
        @Test
        void shouldFailWithBadRequestWhenCaseTypeIdIsNull() throws Exception {
            request = new CaseAssignmentRequest(null, CASE_ID, ASSIGNEE_ID);

            this.mockMvc.perform(post(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(ValidationError.CASE_TYPE_ID_EMPTY)));
        }

        @DisplayName("should fail with 400 bad request when case id is null")
        @Test
        void shouldFailWithBadRequestWhenCaseIdIsNull() throws Exception {
            request = new CaseAssignmentRequest(CASE_TYPE_ID, null, ASSIGNEE_ID);

            this.mockMvc.perform(post(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(ValidationError.CASE_ID_EMPTY)));
        }

        @DisplayName("should fail with 400 bad request when case id is an invalid Luhn number")
        @Test
        void shouldFailWithBadRequestWhenCaseIdIsInvalidLuhnNumber() throws Exception {
            request = new CaseAssignmentRequest(CASE_TYPE_ID, "123", ASSIGNEE_ID);

            this.mockMvc.perform(post(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors", hasItem(ValidationError.CASE_ID_INVALID_LENGTH)))
                .andExpect(jsonPath("$.errors", hasItem(ValidationError.CASE_ID_INVALID)));
        }

        @DisplayName("should fail with 400 bad request when assignee id is empty")
        @Test
        void shouldFailWithBadRequestWhenAssigneeIdIsNull() throws Exception {
            request = new CaseAssignmentRequest(CASE_TYPE_ID, CASE_ID, "");

            this.mockMvc.perform(post(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(ValidationError.ASSIGNEE_ID_EMPTY)));
        }
    }

    @Nested
    @DisplayName("GET /case-assignments")
    class GetCaseAssignments extends BaseMvcTest {

        @DisplayName("happy path test without mockMvc")
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

            this.mockMvc.perform(get(CASE_ASSIGNMENTS_PATH)
                                     .queryParam("case_ids", caseIds))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status_message", is(GET_ASSIGNMENTS_MESSAGE)))
                .andExpect(jsonPath("$.case_assignments", hasSize(1)))
                .andExpect(jsonPath("$.case_assignments[0].case_id", is(TestFixtures.CASE_ID)))
                .andExpect(jsonPath("$.case_assignments[0].shared_with", hasSize(1)))
                .andExpect(jsonPath("$.case_assignments[0].shared_with[0].first_name", is(TestFixtures.FIRST_NAME)))
                .andExpect(jsonPath("$.case_assignments[0].shared_with[0].last_name", is(TestFixtures.LAST_NAME)))
                .andExpect(jsonPath("$.case_assignments[0].shared_with[0].email", is(TestFixtures.EMAIL)))
                .andExpect(jsonPath("$.case_assignments[0].shared_with[0].case_roles", hasSize(2)))
                .andExpect(jsonPath("$.case_assignments[0].shared_with[0].case_roles[0]", is(TestFixtures.CASE_ROLE)))
                .andExpect(jsonPath("$.case_assignments[0].shared_with[0].case_roles[1]", is(TestFixtures.CASE_ROLE2)));
        }

        @DisplayName("should fail with 400 bad request when caseIds query param is not passed")
        @Test
        void shouldFailWithBadRequestWhenCaseIdsInGetAssignmentsIsNull() throws Exception {

            this.mockMvc.perform(get(CASE_ASSIGNMENTS_PATH))
                .andExpect(status().isBadRequest());
        }

        @DisplayName("should fail with 400 bad request when caseIds is empty")
        @Test
        void shouldFailWithBadRequestWhenCaseIdsInGetAssignmentsIsEmpty() throws Exception {

            this.mockMvc.perform(get(CASE_ASSIGNMENTS_PATH)
                                     .queryParam("case_ids", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                    "$.message",
                    containsString("case_ids must be a non-empty list of proper case ids.")
                ));
        }

        @DisplayName("should fail with 400 bad request when caseIds is malformed or invalid")
        @Test
        void shouldFailWithBadRequestWhenCaseIdsInGetAssignmentsIsMalformed() throws Exception {

            this.mockMvc.perform(get(CASE_ASSIGNMENTS_PATH)
                                     .queryParam("case_ids", "121324,%12345"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Case ID should contain digits only")));
        }
    }

    @Nested
    @DisplayName("DELETE /case-assignments")
    class UnassignAccessWithinOrganisation extends BaseMvcTest {

        @Captor
        private ArgumentCaptor<ArrayList<RequestedCaseUnassignment>> requestedCaseUnassignmentsCaptor;

        @DisplayName("happy path test without mockMvc")
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
            final MvcResult result = this.mockMvc.perform(delete(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status_message", is(
                    CaseAssignmentController.UNASSIGN_ACCESS_MESSAGE)))
                .andReturn();

            // ASSERT
            assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
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
            this.mockMvc.perform(delete(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

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

            // ACT + ASSERT
            this.mockMvc.perform(delete(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(ValidationError.EMPTY_REQUESTED_UNASSIGNMENTS_LIST)));
        }

        @DisplayName("should fail with 400 bad request when unassignments list is empty")
        @Test
        void shouldFailWithBadRequestWhenUnassignmentsListIsEmpty() throws Exception {
            // ARRANGE
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(List.of());

            // ACT + ASSERT
            this.mockMvc.perform(delete(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(ValidationError.EMPTY_REQUESTED_UNASSIGNMENTS_LIST)));
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
            this.mockMvc.perform(delete(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(ValidationError.CASE_ID_EMPTY)));
        }

        @DisplayName("should fail with 400 bad request when case id is invalid")
        @Test
        void shouldFailWithBadRequestWhenCaseIdIsInvalid() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                new RequestedCaseUnassignment("invalid", ASSIGNEE_ID, List.of())
            );
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // ACT + ASSERT
            this.mockMvc.perform(delete(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors", hasItem(ValidationError.CASE_ID_INVALID_LENGTH)))
                .andExpect(jsonPath("$.errors", hasItem(ValidationError.CASE_ID_INVALID)));
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
            this.mockMvc.perform(delete(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(ValidationError.ASSIGNEE_ID_EMPTY)));
        }

        @DisplayName("should fail with 400 bad request when assignee id is empty")
        @Test
        void shouldFailWithBadRequestWhenAssigneeIdIsEmpty() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                new RequestedCaseUnassignment(CASE_ID, "", List.of())
            );
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // ACT + ASSERT
            this.mockMvc.perform(delete(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(ValidationError.ASSIGNEE_ID_EMPTY)));
        }

        @DisplayName("should fail with 400 bad request when a case role is invalid")
        @Test
        void shouldFailWithBadRequestWhenACaseRoleIsInvalid() throws Exception {
            // ARRANGE
            List<RequestedCaseUnassignment> unassignments = List.of(
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of("[CR1]", "INVALID"))
            );
            CaseUnassignmentRequest request = new CaseUnassignmentRequest(unassignments);

            // ACT + ASSERT
            this.mockMvc.perform(delete(CASE_ASSIGNMENTS_PATH)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(ValidationError.CASE_ROLE_FORMAT_INVALID)));
        }
    }
}
