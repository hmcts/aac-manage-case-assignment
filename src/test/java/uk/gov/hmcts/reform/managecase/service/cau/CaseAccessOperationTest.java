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
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.datastore.SupplementaryDataResource;
import uk.gov.hmcts.reform.managecase.client.datastore.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CaseAccessOperationTest {

    @Mock(lenient = true)
    private DataStoreApiClient dataStoreApiClient;

    @Mock
    private RoleAssignmentService roleAssignmentService;

    @InjectMocks
    private CaseAccessOperation caseAccessOperation;

    @Mock
    private SecurityUtils securityUtils;

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
    private static final String USER_TOKEN = "1234567890";

    @Nested()
    @DisplayName("removeCaseUserRoles(caseUserRoles)")
    class RemoveCaseAssignUserRoles {

        @Captor
        private ArgumentCaptor<List<RoleAssignmentsDeleteRequest>> deleteRequestsCaptor;

        @Captor
        private ArgumentCaptor<SupplementaryDataUpdateRequest> updateSupplementaryArgumentCaptor;

        private SupplementaryDataUpdateRequest supplementaryDataUpdateRequest;
        private SupplementaryDataUpdateRequest supplementaryDataUpdateRequestOther;

        @BeforeEach
        void setup() {
            given(securityUtils.getUserBearerToken()).willReturn(USER_TOKEN);



            SupplementaryDataResource resource = new SupplementaryDataResource();
            Map<String, Object> resourceMap = new HashMap<>();
            resourceMap.put(getOrgUserCountSupDataKey(ORGANISATION), 4);
            resourceMap.put(getOrgUserCountSupDataKey(ORGANISATION_OTHER), 3);
            resource.setResponse(resourceMap);

            CaseDetails caseDetails = CaseDetails.builder()
                .id(String.valueOf(CASE_ID))
                .reference(CASE_REFERENCE)
                .build();

            CaseDetails caseDetailsOther = CaseDetails.builder()
                .id(String.valueOf(CASE_ID_OTHER))
                .reference(CASE_REFERENCE_OTHER)
                .build();

            doReturn(caseDetailsOther).when(dataStoreApiClient)
                .getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN, String.valueOf(CASE_REFERENCE_OTHER));
            doReturn(caseDetails).when(dataStoreApiClient)
                .getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN, String.valueOf(CASE_REFERENCE));

            Map<String, Object> orgIdToCountMap = new HashMap<>();
            orgIdToCountMap.put(getOrgUserCountSupDataKey(ORGANISATION), -1L);
            supplementaryDataUpdateRequest = new SupplementaryDataUpdateRequest();
            supplementaryDataUpdateRequest.setIncOperation(orgIdToCountMap);

            Map<String, Object> orgIdToCountMapOther = new HashMap<>();
            orgIdToCountMapOther.put(getOrgUserCountSupDataKey(ORGANISATION), -1L);
            orgIdToCountMapOther.put(getOrgUserCountSupDataKey(ORGANISATION_OTHER), -1L);
            supplementaryDataUpdateRequestOther = new SupplementaryDataUpdateRequest();
            supplementaryDataUpdateRequestOther.setIncOperation(orgIdToCountMapOther);
        }

        @Test
        @DisplayName("should remove single case user role")
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
            verify(dataStoreApiClient, times(1)).getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN,
                                                                                      CASE_REFERENCE.toString());
            verify(dataStoreApiClient, never()).updateCaseSupplementaryData(any(), any());
        }

        @Test
        @DisplayName("should remove single [CREATOR] case user role")
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
            verify(dataStoreApiClient, times(1)).getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN,
                                                                                      CASE_REFERENCE.toString());
            verify(dataStoreApiClient, never()).updateCaseSupplementaryData(any(), any());

        }

        @Test
        @DisplayName("should remove multiple case user roles")
        void shouldRemoveMultipleCaseUserRolesForRA() {

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
            verify(dataStoreApiClient, times(1)).getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN,
                                                                                      CASE_REFERENCE.toString());
            verify(dataStoreApiClient, times(1)).getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN,
                                                                                      CASE_REFERENCE_OTHER.toString());
            verify(dataStoreApiClient, never()).updateCaseSupplementaryData(any(), any());
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
            verify(dataStoreApiClient, times(1)).getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN,
                                                                                      CASE_NOT_FOUND.toString());
            verify(dataStoreApiClient, never()).updateCaseSupplementaryData(any(), any());
        }

        @Test
        @DisplayName("should decrement organisation user count for single new case-user relationship")
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

            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture());

            List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
            assertAll(
                () -> assertEquals(1, deleteRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE),
                    deleteRequests.get(0)
                )
            );

            // ASSERT
            verify(dataStoreApiClient).updateCaseSupplementaryData(
                anyString(),
                updateSupplementaryArgumentCaptor.capture()
            );
            SupplementaryDataUpdateRequest request = updateSupplementaryArgumentCaptor.getValue();
            assertEquals(supplementaryDataUpdateRequest.getIncOperation(), request.getIncOperation());
            verify(dataStoreApiClient, times(1)).getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN,
                                                                                      CASE_REFERENCE.toString());
            verify(dataStoreApiClient, times(1))
                .updateCaseSupplementaryData(CASE_REFERENCE.toString(), supplementaryDataUpdateRequest);
        }

        @Test
        @DisplayName(
            "should not decrement organisation user count for non-existing case-user relationship"
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
            verify(roleAssignmentService, times(1)).deleteRoleAssignments(new ArrayList<>());
            verify(dataStoreApiClient, times(1)).getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN,
                                                                                      CASE_REFERENCE.toString());
            verify(dataStoreApiClient, never()).updateCaseSupplementaryData(any(), any());
        }

        @Test
        @DisplayName(
            "should not decrement organisation user count for single new case-user with [CREATOR] role"
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
            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture());

            List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
            assertAll(
                () -> assertEquals(1, deleteRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE_CREATOR),
                    deleteRequests.get(0)
                )
            );

            verify(dataStoreApiClient, times(1)).getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN,
                                                                                      CASE_REFERENCE.toString());
            verify(dataStoreApiClient, never()).updateCaseSupplementaryData(anyString(), any());
        }

        @Test
        @DisplayName("should decrement organisation user count only once for repeat remove case-user relationship"
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
            verify(dataStoreApiClient).updateCaseSupplementaryData(
                anyString(),
                updateSupplementaryArgumentCaptor.capture()
            );
            SupplementaryDataUpdateRequest request = updateSupplementaryArgumentCaptor.getValue();
            assertEquals(supplementaryDataUpdateRequest.getIncOperation(), request.getIncOperation());

            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture()
            );
            List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
            assertAll(
                () -> assertEquals(1, deleteRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE, CASE_ROLE),
                    deleteRequests.get(0)
                )
            );
            verify(dataStoreApiClient, times(1))
                .updateCaseSupplementaryData(CASE_REFERENCE.toString(), supplementaryDataUpdateRequest);
            verify(dataStoreApiClient, times(1)).getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN,
                                                                                      CASE_REFERENCE.toString());
        }

        @Test
        @DisplayName("should decrement organisation user count only once and ignore creator role")
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
            verify(dataStoreApiClient).updateCaseSupplementaryData(
                anyString(),
                updateSupplementaryArgumentCaptor.capture()
            );

            SupplementaryDataUpdateRequest request = updateSupplementaryArgumentCaptor.getValue();
            assertEquals(supplementaryDataUpdateRequest.getIncOperation(), request.getIncOperation());

            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture()
            );
            List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
            assertAll(
                () -> assertEquals(1, deleteRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE, CASE_ROLE_CREATOR),
                    deleteRequests.get(0)
                )
            );
            verify(dataStoreApiClient, times(1))
                .updateCaseSupplementaryData(CASE_REFERENCE.toString(), supplementaryDataUpdateRequest);
            verify(dataStoreApiClient, times(1)).getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN,
                                                                                      CASE_REFERENCE.toString());
        }

        @Test
        @DisplayName(
            "should decrement organisation user count for single creator role after removing other roles"
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
            verify(dataStoreApiClient).updateCaseSupplementaryData(
                anyString(),
                updateSupplementaryArgumentCaptor.capture()
            );

            SupplementaryDataUpdateRequest request = updateSupplementaryArgumentCaptor.getValue();
            assertEquals(supplementaryDataUpdateRequest.getIncOperation(), request.getIncOperation());

            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture()
            );
            List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
            assertAll(
                () -> assertEquals(1, deleteRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE),
                    deleteRequests.get(0)
                )
            );
            verify(dataStoreApiClient, times(1))
                .updateCaseSupplementaryData(CASE_REFERENCE.toString(), supplementaryDataUpdateRequest);
            verify(dataStoreApiClient, times(1)).getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN,
                                                                                      CASE_REFERENCE.toString());
        }

        @Test
        @DisplayName("should not decrement organisation user count after removing creator role")
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
            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture()
            );
            List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
            assertAll(
                () -> assertEquals(1, deleteRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE_CREATOR),
                    deleteRequests.get(0)
                )
            );
            verify(dataStoreApiClient, never()).updateCaseSupplementaryData(any(), any());
            verify(dataStoreApiClient, times(1)).getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN,
                                                                                      CASE_REFERENCE.toString());
        }

        @Test
        @DisplayName(
            "should not decrement organisation user count for an existing relationship with another role"
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
            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture()
            );
            List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
            assertAll(
                () -> assertEquals(1, deleteRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE),
                    deleteRequests.get(0)
                )
            );
            verify(dataStoreApiClient, never()).updateCaseSupplementaryData(any(), any());
            verify(dataStoreApiClient, times(1)).getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN,
                                                                                      CASE_REFERENCE.toString());
        }

        @Test
        @DisplayName("should decrement organisation user count for multiple case-user relationship")
        void shouldDecrementOrganisationUserCountForMultipleRelationshipsForRA() {

            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                // CASE_REFERENCE/CASE_ID
                // (2 orgs with 2 users with 2 roles >> 2 org counts decremented by 1)
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE, ORGANISATION),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID_OTHER, CASE_ROLE,
                                                         ORGANISATION
                ),
                new CaseAssignedUserRoleWithOrganisation(CASE_REFERENCE.toString(), USER_ID, CASE_ROLE_OTHER,
                                                         ORGANISATION_OTHER
                )
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
            verify(dataStoreApiClient).updateCaseSupplementaryData(
                anyString(),
                updateSupplementaryArgumentCaptor.capture()
            );
            SupplementaryDataUpdateRequest request = updateSupplementaryArgumentCaptor.getValue();
            assertEquals(supplementaryDataUpdateRequestOther.getIncOperation(), request.getIncOperation());

            verify(roleAssignmentService).deleteRoleAssignments(deleteRequestsCaptor.capture()
            );
            List<RoleAssignmentsDeleteRequest> deleteRequests = deleteRequestsCaptor.getValue();
            assertAll(
                () -> assertEquals(2, deleteRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID, List.of(CASE_ROLE, CASE_ROLE_OTHER),
                    deleteRequests.get(0)
                ),
                () -> assertCorrectlyPopulatedRoleAssignmentsDeleteRequest(
                    CASE_REFERENCE.toString(), USER_ID_OTHER, List.of(CASE_ROLE),
                    deleteRequests.get(1)
                )
            );
            verify(dataStoreApiClient, times(1)).getCaseDetailsByCaseIdViaExternalApi(USER_TOKEN,
                                                                                      CASE_REFERENCE.toString());
        }
    }

    @Nested()
    class GetCaseAssignUserRoles {

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
            argThat(arg -> arg.contains(USER_ID) || arg.isEmpty())
                    )
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

