package uk.gov.hmcts.reform.managecase.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.managecase.TestFixtures;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseCouldNotBeFoundException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;
import uk.gov.hmcts.reform.managecase.api.payload.RequestedCaseUnassignment;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignedUsers;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import jakarta.validation.ValidationException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.caseDetails;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.organisationPolicy;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.user;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.usersByOrganisation;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_NOT_FOUND;

@SuppressWarnings({"PMD.MethodNamingConventions",
    "PMD.JUnitAssertionsShouldIncludeMessage",
    "PMD.ExcessiveImports"})
class CaseAssignmentServiceTest {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String ASSIGNEE_ID = "ae2eb34c-816a-4eea-b714-6654d022fcef";
    private static final String ASSIGNEE_ID2 = "38130f09-0010-4c12-afd1-2563bb25d1d3";
    private static final String ANOTHER_USER = "vcd345cvs-816a-4eea-b714-6654d022f0ef";
    private static final String CASE_ID = "12345678";
    private static final String CASE_ID2 = "87654321";
    private static final String ORG_POLICY_ROLE = "caseworker-probate";
    private static final String ORG_POLICY_ROLE2 = "caseworker-probate2";
    private static final String CASE_ROLE = "[CR1]";
    private static final String CASE_ROLE2 = "[CR2]";
    private static final String ORGANIZATION_ID = "TEST_ORG";

    private static final String BEAR_TOKEN = "TestBearToken";

    @InjectMocks
    private CaseAssignmentService service;

    @Mock
    private DataStoreRepository dataStoreRepository;
    @Mock
    private PrdRepository prdRepository;
    @Mock
    private IdamRepository idamRepository;
    @Mock
    private JacksonUtils jacksonUtils;
    @Mock
    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Nested
    @DisplayName("Assign Case Access")
    class AssignCaseAccess {

        private CaseAssignment caseAssignment;

        @BeforeEach
        void setUp() {
            caseAssignment = new CaseAssignment(CASE_TYPE_ID, CASE_ID, ASSIGNEE_ID);

            given(dataStoreRepository.findCaseByCaseIdUsingExternalApi(CASE_ID))
                .willReturn(caseDetails(ORGANIZATION_ID, ORG_POLICY_ROLE));
            given(prdRepository.findUsersByOrganisation())
                .willReturn(usersByOrganisation(user(ASSIGNEE_ID)));
            given(securityUtils.hasSolicitorRole(anyList())).willReturn(true);

            UserDetails userDetails = UserDetails.builder()
                .id(ASSIGNEE_ID).roles(List.of("caseworker-AUTOTEST1-solicitor")).build();
            given(idamRepository.getCaaSystemUserAccessToken()).willReturn(BEAR_TOKEN);
            given(idamRepository.getUserByUserId(ASSIGNEE_ID, BEAR_TOKEN)).willReturn(userDetails);
        }

        @Test
        @DisplayName("should assign case in the organisation")
        void shouldAssignCaseAccess() {

            given(securityUtils.hasSolicitorAndJurisdictionRoles(anyList(), anyString())).willReturn(true);
            given(dataStoreRepository.findCaseByCaseIdUsingExternalApi(CASE_ID))
                .willReturn(caseDetails(ORGANIZATION_ID, ORG_POLICY_ROLE, ORG_POLICY_ROLE2));

            given(jacksonUtils.convertValue(any(JsonNode.class), eq(OrganisationPolicy.class)))
                .willReturn(organisationPolicy(ORGANIZATION_ID, ORG_POLICY_ROLE))
                .willReturn(organisationPolicy(ORGANIZATION_ID, ORG_POLICY_ROLE2));

            List<String> roles = service.assignCaseAccess(caseAssignment, true);

            assertThat(roles).containsExactly(ORG_POLICY_ROLE, ORG_POLICY_ROLE2);

            verify(dataStoreRepository)
                .assignCase(List.of(ORG_POLICY_ROLE, ORG_POLICY_ROLE2), CASE_ID, ASSIGNEE_ID, ORGANIZATION_ID);
            verify(dataStoreRepository)
                .findCaseByCaseIdUsingExternalApi(CASE_ID);
        }

        @Test
        @DisplayName("should assign case in the organisation when invoked by non solicitor user")
        void shouldAssignCaseAccessWhenInvokedByNonSolicitorUser() {

            given(securityUtils.hasSolicitorAndJurisdictionRoles(anyList(), anyString())).willReturn(true);
            given(dataStoreRepository.findCaseByCaseIdAsSystemUserUsingExternalApi(CASE_ID))
                .willReturn(caseDetails(ORGANIZATION_ID, ORG_POLICY_ROLE, ORG_POLICY_ROLE2));

            given(jacksonUtils.convertValue(any(JsonNode.class), eq(OrganisationPolicy.class)))
                .willReturn(organisationPolicy(ORGANIZATION_ID, ORG_POLICY_ROLE))
                .willReturn(organisationPolicy(ORGANIZATION_ID, ORG_POLICY_ROLE2));

            List<String> roles = service.assignCaseAccess(caseAssignment, false);

            assertThat(roles).containsExactly(ORG_POLICY_ROLE, ORG_POLICY_ROLE2);

            verify(dataStoreRepository)
                .assignCase(List.of(ORG_POLICY_ROLE, ORG_POLICY_ROLE2), CASE_ID, ASSIGNEE_ID, ORGANIZATION_ID);
            verify(dataStoreRepository)
                .findCaseByCaseIdAsSystemUserUsingExternalApi(CASE_ID);
        }

        @Test
        @DisplayName("should throw case could not be found error when case is not found")
        void shouldThrowCaseCouldNotBeFetchedException_whenCaseNotFound() {

            given(dataStoreRepository.findCaseByCaseIdUsingExternalApi(CASE_ID))
                .willThrow(new CaseCouldNotBeFoundException(CASE_NOT_FOUND));

            assertThatThrownBy(() -> service.assignCaseAccess(caseAssignment, true))
                .isInstanceOf(CaseCouldNotBeFoundException.class)
                .hasMessageContaining(CASE_NOT_FOUND);
        }

        @Test
        @DisplayName("should throw validation error when assignee is not found in the organisation")
        void shouldThrowValidationException_whenAssigneeNotExists() {

            given(prdRepository.findUsersByOrganisation())
                .willReturn(usersByOrganisation(user(ANOTHER_USER)));

            assertThatThrownBy(() -> service.assignCaseAccess(caseAssignment, true))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(ValidationError.ASSIGNEE_ORGANISATION_ERROR);
        }

        @Test
        @DisplayName("should throw validation error when assignee doesn't have jurisdiction solicitor role")
        void shouldThrowValidationException_whenAssigneeRolesNotMatching() {

            UserDetails userDetails = UserDetails.builder()
                .id(ASSIGNEE_ID).roles(List.of("caseworker-AUTOTEST2-solicitor")).build();

            given(idamRepository.getUserByUserId(ASSIGNEE_ID, BEAR_TOKEN)).willReturn(userDetails);

            assertThatThrownBy(() -> service.assignCaseAccess(caseAssignment, true))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(ValidationError.ASSIGNEE_ROLE_ERROR);
        }
    }

    @Nested
    @DisplayName("Get Case Access")
    class GetCaseAssignments {

        @BeforeEach
        void setUp() {
            openMocks(this);
        }

        @Test
        @DisplayName("should get case assignments")
        void shouldGetCaseAssignments() {
            // ARRANGE
            given(prdRepository.findUsersByOrganisation())
                .willReturn(usersByOrganisation(user(ASSIGNEE_ID), user(ASSIGNEE_ID2)));
            given(dataStoreRepository.getCaseAssignments(eq(List.of(CASE_ID, CASE_ID2)),
                                                         eq(List.of(ASSIGNEE_ID, ASSIGNEE_ID2))))
                .willReturn(List.of(
                    new CaseUserRole(CASE_ID, ASSIGNEE_ID, CASE_ROLE),
                    new CaseUserRole(CASE_ID2, ASSIGNEE_ID2, CASE_ROLE),
                    new CaseUserRole(CASE_ID2, ASSIGNEE_ID2, CASE_ROLE2)));

            // ACT
            List<CaseAssignedUsers> response = service.getCaseAssignments(List.of(CASE_ID, CASE_ID2));

            // ASSERT
            assertThat(response).hasSize(2);
            assertThat(response.get(0).getCaseId()).isEqualTo(CASE_ID);
            assertThat(response.get(0).getUsers()).hasSize(1);
            assertThat(response.get(0).getUsers().get(0).getIdamId()).isEqualTo(ASSIGNEE_ID);
            assertThat(response.get(0).getUsers().get(0).getCaseRoles()).hasSize(1);
            assertThat(response.get(0).getUsers().get(0).getCaseRoles().get(0)).isEqualTo(CASE_ROLE);
            assertThat(response.get(0).getUsers().get(0).getEmail()).isEqualTo(TestFixtures.EMAIL);
            assertThat(response.get(0).getUsers().get(0).getFirstName()).isEqualTo(TestFixtures.FIRST_NAME);
            assertThat(response.get(0).getUsers().get(0).getLastName()).isEqualTo(TestFixtures.LAST_NAME);
            assertThat(response.get(1).getCaseId()).isEqualTo(CASE_ID2);
            assertThat(response.get(1).getUsers()).hasSize(1);
            assertThat(response.get(1).getUsers().get(0).getIdamId()).isEqualTo(ASSIGNEE_ID2);
            assertThat(response.get(1).getUsers().get(0).getCaseRoles()).hasSize(2);
            assertThat(response.get(1).getUsers().get(0).getCaseRoles().get(0)).isEqualTo(CASE_ROLE);
            assertThat(response.get(1).getUsers().get(0).getCaseRoles().get(1)).isEqualTo(CASE_ROLE2);
            assertThat(response.get(1).getUsers().get(0).getEmail()).isEqualTo(TestFixtures.EMAIL);
            assertThat(response.get(1).getUsers().get(0).getFirstName()).isEqualTo(TestFixtures.FIRST_NAME);
            assertThat(response.get(1).getUsers().get(0).getLastName()).isEqualTo(TestFixtures.LAST_NAME);
        }
    }

    @Nested
    @DisplayName("Unassign Case Access")
    class UnassignCaseAccess {

        @Captor
        private ArgumentCaptor<ArrayList<CaseUserRole>> caseUserRolesCaptor;

        private List<RequestedCaseUnassignment> unassignments;

        @BeforeEach
        void setUp() {
            openMocks(this);

            given(prdRepository.findUsersByOrganisation())
                .willReturn(usersByOrganisation(user(ASSIGNEE_ID), user(ASSIGNEE_ID2)));
        }

        @Test
        @DisplayName("should un-assign a single case role in the organisation")
        void shouldUnAssignSingleCaseRole() {
            // ARRANGE
            unassignments = List.of(
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of(CASE_ROLE))
            );

            // ACT
            service.unassignCaseAccess(unassignments);

            // ASSERT
            verify(dataStoreRepository).removeCaseUserRoles(caseUserRolesCaptor.capture(), eq(ORGANIZATION_ID));
            List<CaseUserRole> captorValue = caseUserRolesCaptor.getValue();
            assertThat(captorValue).hasSize(1);
            assertThat(captorValue.get(0).getCaseId()).isEqualTo(CASE_ID);
            assertThat(captorValue.get(0).getUserId()).isEqualTo(ASSIGNEE_ID);
            assertThat(captorValue.get(0).getCaseRole()).isEqualTo(CASE_ROLE);
        }

        @Test
        @DisplayName("should un-assign multiple case roles in the organisation")
        void shouldUnAssignMultipleCaseRole() {
            // ARRANGE
            unassignments = List.of(
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of(CASE_ROLE)),
                new RequestedCaseUnassignment(CASE_ID2, ASSIGNEE_ID, List.of(CASE_ROLE, CASE_ROLE2))
            );

            CaseUserRole expectedCaseUserRole1 = new CaseUserRole(CASE_ID, ASSIGNEE_ID, CASE_ROLE);
            CaseUserRole expectedCaseUserRole2 = new CaseUserRole(CASE_ID2, ASSIGNEE_ID, CASE_ROLE);
            CaseUserRole expectedCaseUserRole3 = new CaseUserRole(CASE_ID2, ASSIGNEE_ID, CASE_ROLE2);

            // ACT
            service.unassignCaseAccess(unassignments);

            // ASSERT
            verify(dataStoreRepository).removeCaseUserRoles(caseUserRolesCaptor.capture(), eq(ORGANIZATION_ID));
            List<CaseUserRole> captorValue = caseUserRolesCaptor.getValue();
            assertThat(captorValue)
                .hasSize(3)
                .contains(expectedCaseUserRole1)
                .contains(expectedCaseUserRole2)
                .contains(expectedCaseUserRole3);
        }

        @Test
        @DisplayName("Should looking up role information if not supplied: and should un-assign if roles returned")
        void shouldLookUpRolesAndUnAssign() {
            // ARRANGE
            unassignments = List.of(
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of()),
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID2, List.of()),
                new RequestedCaseUnassignment(CASE_ID2, ASSIGNEE_ID, List.of()),
                new RequestedCaseUnassignment(CASE_ID2, ASSIGNEE_ID2, List.of())
            );

            List<CaseUserRole> allCaseUserRoles = List.of(
                new CaseUserRole(CASE_ID, ASSIGNEE_ID, CASE_ROLE),
                new CaseUserRole(CASE_ID, ASSIGNEE_ID2, CASE_ROLE2),
                new CaseUserRole(CASE_ID2, ASSIGNEE_ID, CASE_ROLE),
                new CaseUserRole(CASE_ID2, ASSIGNEE_ID2, CASE_ROLE),
                new CaseUserRole(CASE_ID2, ASSIGNEE_ID, CASE_ROLE2),
                new CaseUserRole(CASE_ID2, ASSIGNEE_ID2, CASE_ROLE2)
            );

            given(dataStoreRepository.getCaseAssignments(eq(List.of(CASE_ID, CASE_ID2)),
                                                         eq(List.of(ASSIGNEE_ID, ASSIGNEE_ID2))))
                .willReturn(allCaseUserRoles);

            // ACT
            service.unassignCaseAccess(unassignments);

            // ASSERT
            // verify look up of case roles occurred
            verify(dataStoreRepository, times(1))
                .getCaseAssignments(eq(List.of(CASE_ID, CASE_ID2)), eq(List.of(ASSIGNEE_ID, ASSIGNEE_ID2)));
            // verify action taken
            verify(dataStoreRepository).removeCaseUserRoles(caseUserRolesCaptor.capture(), eq(ORGANIZATION_ID));
            List<CaseUserRole> captorValue = caseUserRolesCaptor.getValue();
            assertThat(captorValue)
                .hasSameSizeAs(allCaseUserRoles)
                .containsAll(allCaseUserRoles);
        }

        @Test
        @DisplayName("Should looking up role information if not supplied: and should NOT un-assign if NO roles found")
        void shouldLookUpRolesAndNotUnAssignIfNonFound() {
            // ARRANGE
            unassignments = List.of(
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of()),
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID2, List.of()),
                new RequestedCaseUnassignment(CASE_ID2, ASSIGNEE_ID, List.of()),
                new RequestedCaseUnassignment(CASE_ID2, ASSIGNEE_ID2, List.of())
            );

            given(dataStoreRepository.getCaseAssignments(eq(List.of(CASE_ID, CASE_ID2)),
                                                         eq(List.of(ASSIGNEE_ID, ASSIGNEE_ID2))))
                .willReturn(List.of()); // returns empty: i.e. non-found

            // ACT
            service.unassignCaseAccess(unassignments);

            // ASSERT
            // verify look up of case roles occurred
            verify(dataStoreRepository, times(1))
                .getCaseAssignments(eq(List.of(CASE_ID, CASE_ID2)), eq(List.of(ASSIGNEE_ID, ASSIGNEE_ID2)));
            // verify no action taken
            verify(dataStoreRepository, never()).removeCaseUserRoles(any(), eq(ORGANIZATION_ID));
        }

        @Test
        @DisplayName("should un-assign case roles after stripping duplicates found through lookups or requests.")
        void shouldUnAssignCaseRoleWithoutDuplicates() {
            // ARRANGE
            unassignments = List.of(
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of(CASE_ROLE)),
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of(CASE_ROLE)), // i.e. DUPLICATE
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, null),
                new RequestedCaseUnassignment(CASE_ID, ASSIGNEE_ID, List.of())
            );

            List<CaseUserRole> allCaseUserRoles = List.of(
                new CaseUserRole(CASE_ID, ASSIGNEE_ID, CASE_ROLE) // i.e. DUPLICATE
            );

            given(dataStoreRepository.getCaseAssignments(eq(List.of(CASE_ID)),
                                                         eq(List.of(ASSIGNEE_ID))))
                .willReturn(allCaseUserRoles);

            // ACT
            service.unassignCaseAccess(unassignments);

            // ASSERT
            // verify look up of case roles occurred
            verify(dataStoreRepository, times(1))
                .getCaseAssignments(eq(List.of(CASE_ID)), eq(List.of(ASSIGNEE_ID)));
            // verify action taken
            verify(dataStoreRepository).removeCaseUserRoles(caseUserRolesCaptor.capture(), eq(ORGANIZATION_ID));
            List<CaseUserRole> captorValue = caseUserRolesCaptor.getValue();
            assertThat(captorValue)
                .hasSize(1) // just the one although multiple requested
                .contains(new CaseUserRole(CASE_ID, ASSIGNEE_ID, CASE_ROLE));
        }

        @Test
        @DisplayName("should throw validation error when assignee is not found in the organisation")
        void shouldThrowValidationException_whenAssigneeNotInOrganisation() {
            // ARRANGE
            unassignments = List.of(
                new RequestedCaseUnassignment(CASE_ID, "user-not-in-org", List.of(CASE_ROLE))
            );

            // ACT + ASSERT
            assertThatThrownBy(() -> service.unassignCaseAccess(unassignments))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(ValidationError.UNASSIGNEE_ORGANISATION_ERROR);
        }
    }
}
