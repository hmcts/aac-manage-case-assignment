package uk.gov.hmcts.reform.managecase.service.cau;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentsDeleteRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetailsExtended;
import uk.gov.hmcts.reform.managecase.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.reform.managecase.data.casedetails.supplementarydata.SupplementaryDataRepository;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CaseAccessOperationTest {

    @Mock(lenient = true)
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private SupplementaryDataRepository supplementaryDataRepository;

    @Mock
    private RoleAssignmentService roleAssignmentService;

    @InjectMocks
    private CaseAccessOperation caseAccessOperation;

    private static final String JURISDICTION = "CMC";
    private static final String WRONG_JURISDICTION = "DIVORCE";
    private static final Long CASE_REFERENCE = 1234123412341236L;
    private static final Long CASE_REFERENCE_OTHER = 1111222233334444L;
    private static final String USER_ID = "123";
    private static final String USER_ID_OTHER = "USER_ID_OTHER";
    private static final Long CASE_ID = 456L;
    private static final Long CASE_ID_OTHER = 1234L;
    private static final Long CASE_NOT_FOUND = 9999999999999999L;
    private static final String CASE_ROLE = "[DEFENDANT]";
    private static final String CASE_ROLE_OTHER = "[OTHER]";
    private static final String CASE_ROLE_CREATOR = "[CREATOR]";
    private static final String ORGANISATION = "ORGANISATION";
    private static final String ORGANISATION_OTHER = "ORGANISATION_OTHER";

    @BeforeEach
    void setUp() {
        configureCaseRepository(JURISDICTION);
    }

    @Nested()
    @DisplayName("removeCaseUserRoles(caseUserRoles)")
    class RemoveCaseAssignUserRoles {

        @Captor
        private ArgumentCaptor<List<RoleAssignmentsDeleteRequest>> deleteRequestsCaptor;

        @BeforeEach
        void setUp() {
            configureCaseRepository(null);
        }

        @Test
        @DisplayName("RA set to true, should remove single case user role")
        void shouldRemoveSingleCaseUserRoleForRA() {


            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture());

            List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
            assertAll(
                () -> assertEquals(1, deleteRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE),
                    deleteRequests.get(0)
                )
            );
        }

        @Test
        @DisplayName("RA set to true, should remove single [CREATOR] case user role")
        void shouldRemoveSingleCreatorCaseUserRoleForRA() {

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture());

            List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
            assertAll(
                () -> assertEquals(1, deleteRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE_CREATOR),
                    deleteRequests.get(0)
                )
            );
        }

        @Test
        @DisplayName("RA set to true, should remove multiple case user roles")
        void shouldRemoveMultipleCaseUserRolesForRA() {

            final CaseDetailsExtended caseDetailsOther = new CaseDetailsExtended();
            caseDetailsOther.setId(String.valueOf(CASE_ID_OTHER));
            caseDetailsOther.setReference(CASE_REFERENCE_OTHER);
            doReturn(Optional.of(caseDetailsOther)).when(caseDetailsRepository).findByReference(null,
                                                                                                CASE_REFERENCE_OTHER);

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE_OTHER.toString(), USER_ID, CASE_ROLE)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                    new CaseAssignedUserRole(CASE_REFERENCE_OTHER.toString(), USER_ID, CASE_ROLE)
                ),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture());

            Map<String, RoleAssignmentsDeleteRequest> deleteRequestsMapByCaseId =
                deleteRequestsCaptor.getValue().stream()
                    .collect(Collectors.toMap(
                        RoleAssignmentsDeleteRequest::getCaseId, deleteRequests -> deleteRequests)
                    );

            assertAll(
                () -> assertEquals(2, deleteRequestsMapByCaseId.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE),
                    deleteRequestsMapByCaseId.get(CASE_REFERENCE.toString())
                ),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE_OTHER.toString(), USER_ID, List.of(CASE_ROLE),
                    deleteRequestsMapByCaseId.get(CASE_REFERENCE_OTHER.toString())
                )
            );
        }

        @Test
        @DisplayName("should throw not found exception when case not found")
        void shouldThrowNotFound() {

            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_NOT_FOUND.toString(), USER_ID, CASE_ROLE)
            );

            // ACT / ASSERT
            assertThrows(CaseNotFoundException.class, () -> caseAccessOperation.removeCaseUserRoles(caseUserRoles));

            verifyNoInteractions(roleAssignmentService);
        }

        @Test
        @DisplayName("RA set to true, should decrement organisation user count for single new case-user relationship")
        void shouldDecrementOrganisationUserCountForSingleNewRelationshipForRA() {

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);

        }

        @Test
        @DisplayName(
            "RA set to true, should not decrement organisation user count for non-existing case-user relationship"
        )
        void shouldNotDecrementOrganisationUserCountForExistingRelationshipForRA() {

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // no existing relationship
            mockExistingCaseUserRolesForRA(new ArrayList<>());

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());

        }

        @Test
        @DisplayName(
            "RA set to true, should not decrement organisation user count for single new case-user with [CREATOR] role"
        )
        void shouldNotIncrementOrganisationUserCountWithCreatorRoleForRA() {

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());
        }

        @Test
        @DisplayName(
            "RA set to true, "
                + "should decrement organisation user count only once for repeat remove case-user relationship"
        )
        void shouldDecrementOrganisationUserCountOnlyOnceForRepeatRelationshipForRA() {

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);
        }

        @Test
        @DisplayName("RA set to true, should decrement organisation user count only once and ignore creator role")
        void shouldDecrementOrganisationUserCountOnlyOnceForRepeatRelationshipIgnoreCreatorRoleForRA() {

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)
                ),
                // after
                new ArrayList<>()
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);
        }

        @Test
        @DisplayName(
            "RA set to true, "
                + "should decrement organisation user count for single creator role after removing other roles"
        )
        void shouldDecrementOrganisationUserCountForSingleCreatorRoleAfterRemovingOtherRolesForRA() {

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)
                ),
                // after
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)
                )
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);
        }

        @Test
        @DisplayName("RA set to true, should not decrement organisation user count after removing creator role")
        void shouldNotDecrementOrganisationUserCountAfterRemovingCreatorRoleForRA() {

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(
                    CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_CREATOR)
                ),
                // after
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE)
                )
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());
        }

        @Test
        @DisplayName(
            "RA set to true, "
                + "should not decrement organisation user count for an existing relationship with another role"
        )
        void shouldNotDecrementOrganisationUserCountForAnExistingRelationshipWithOtherRoleForRA() {

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER)
                ),
                // after
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER)
                )
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            verify(supplementaryDataRepository, never()).incrementSupplementaryData(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("RA set to true, should decrement organisation user count for multiple case-user relationship")
        void shouldDecrementOrganisationUserCountForMultipleRelationshipsForRA() {

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                // CASE_REFERENCE/CASE_ID
                // (2 orgs with 2 users with 2 roles >> 2 org counts decremented by 1)
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE,
                                                         ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER,
                                                         ORGANISATION_OTHER)
            );

            // for an existing relation and then after removal
            mockExistingCaseUserRolesForRA(
                // before
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE),
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER),
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE),
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE_OTHER)
                ),
                // after
                List.of(
                    new CaseAssignedUserRole(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE_OTHER)
                )
            );

            // ACT
            caseAccessOperation.removeCaseUserRoles(caseUserRoles);

            // ASSERT
            // verify CASE_REFERENCE/CASE_ID
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION), -1L);
            verify(supplementaryDataRepository, times(1))
                .incrementSupplementaryData(CASE_REFERENCE.toString(), getOrgUserCountSupDataKey(ORGANISATION_OTHER),
                                            -1L);
        }

    }

    @Nested()
    class GetCaseAssignUserRoles {

        @BeforeEach
        void setUp() {
            configureCaseRepository(null);
        }

        @Test
        void shouldGetCaseAssignedUserRoles() {
            List<Long> caseReferences = Lists.newArrayList(CASE_REFERENCE);
            when(roleAssignmentService.findRoleAssignmentsByCasesAndUsers(anyList(), anyList()))
                .thenReturn(getCaseAssignedUserRoles());
            List<CaseAssignedUserRole> caseAssignedUserRoles = caseAccessOperation.findCaseUserRoles(
                caseReferences, Lists.newArrayList());

            assertNotNull(caseAssignedUserRoles);
            assertEquals(1, caseAssignedUserRoles.size());
            assertEquals(CASE_ROLE, caseAssignedUserRoles.get(0).getCaseRole());
        }

        @Test
        void shouldReturnEmptyResultOnNonExistingCases() {
            List<Long> caseReferences = Lists.newArrayList(CASE_NOT_FOUND);
            List<CaseAssignedUserRole> caseAssignedUserRoles = caseAccessOperation.findCaseUserRoles(
                caseReferences, Lists.newArrayList());

            assertNotNull(caseAssignedUserRoles);
            assertEquals(0, caseAssignedUserRoles.size());
        }

        private List<CaseAssignedUserRole> getCaseAssignedUserRoles() {
            return Arrays.asList(
                new CaseAssignedUserRole[]{new CaseAssignedUserRole("caseDataId", "userId", CASE_ROLE)}
            );
        }
    }

    private void configureCaseRepository(String jurisdiction) {
        final CaseDetailsExtended caseDetails = new CaseDetailsExtended();
        caseDetails.setId(String.valueOf(CASE_ID));
        caseDetails.setReference(CASE_REFERENCE);
        final CaseDetailsExtended caseDetailsOther = new CaseDetailsExtended();
        caseDetailsOther.setId(String.valueOf(CASE_ID_OTHER));
        caseDetailsOther.setReference(CASE_REFERENCE_OTHER);

        doReturn(Optional.of(caseDetails)).when(caseDetailsRepository)
            .findByReference(jurisdiction, CASE_REFERENCE);
        doReturn(Optional.of(caseDetailsOther)).when(caseDetailsRepository)
            .findByReference(jurisdiction, CASE_REFERENCE_OTHER);
        doReturn(Optional.empty()).when(caseDetailsRepository)
            .findByReference(jurisdiction, CASE_NOT_FOUND);
        doReturn(Optional.empty()).when(caseDetailsRepository)
            .findByReference(WRONG_JURISDICTION, CASE_REFERENCE);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
        final String expectedCaseId,
        final String expectedUserId,
        final List<String> expectedRoleNames,
        final RoleAssignmentsDeleteRequest actualRoleAssignmentsDeleteRequest
    ) {
        assertNotNull(actualRoleAssignmentsDeleteRequest);
        assertAll(
            () -> assertEquals(expectedCaseId, actualRoleAssignmentsDeleteRequest.getCaseId()),
            () -> assertEquals(expectedUserId, actualRoleAssignmentsDeleteRequest.getUserId()),
            () -> assertEquals(expectedRoleNames.size(), actualRoleAssignmentsDeleteRequest.getRoleNames().size()),
            () -> assertThat(
                actualRoleAssignmentsDeleteRequest.getRoleNames(),
                containsInAnyOrder(expectedRoleNames.toArray())
            )
        );
    }

    private OngoingStubbing<List<CaseAssignedUserRole>> mockExistingCaseUserRolesForRA(
        List<CaseAssignedUserRole> existingCaseUserRoles
    ) {
        return when(roleAssignmentService.findRoleAssignmentsByCasesAndUsers(
            argThat(arg -> arg.contains(CASE_REFERENCE.toString())
                || arg.contains(CASE_REFERENCE_OTHER.toString())),
            argThat(arg -> arg.contains(USER_ID) || arg.isEmpty()))
        ).thenReturn(existingCaseUserRoles);
    }

    private void mockExistingCaseUserRolesForRA(List<CaseAssignedUserRole> existingCaseUserRoles,
                                                List<CaseAssignedUserRole> secondCallCaseUserRoles) {
        mockExistingCaseUserRolesForRA(existingCaseUserRoles)
            .thenReturn(secondCallCaseUserRoles);
    }

    private String getOrgUserCountSupDataKey(String organisationId) {
        return "orgs_assigned_users." + organisationId;
    }
}
