package uk.gov.hmcts.reform.managecase.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.ActorIdType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.Classification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignment;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentAttributes;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentQuery;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentRequestResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignments;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentsDeleteRequest;
import uk.gov.hmcts.reform.managecase.api.payload.RoleType;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.service.casedataaccesscontrol.RoleAssignmentCategoryService;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentService;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentServiceHelper;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentsMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("RoleAssignmentService")
@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceTest {

    private static final String CASE_ID = "111111";
    private static final String USER_ID = "user1";
    private static final String USER_ID_2 = "user2";
    private static final RoleCategory ROLE_CATEGORY_4_USER_1 = RoleCategory.PROFESSIONAL;
    @Mock
    private RoleAssignmentCategoryService roleAssignmentCategoryService;
    @Mock
    private RoleAssignmentServiceHelper roleAssignmentServiceHelper;
    @Mock
    private RoleAssignmentsMapper roleAssignmentsMapper;
    @Mock
    private RoleAssignmentResponse mockedRoleAssignmentResponse;
    @InjectMocks
    private RoleAssignmentService roleAssignmentService;
    private final List<String> caseIds = Arrays.asList("111", "222");
    private final List<String> userIds = Arrays.asList("111", "222");

    @Test
    public void shouldGetRoleAssignmentsByCasesAndUsers() {

        given(roleAssignmentServiceHelper.findRoleAssignmentsByCasesAndUsers(caseIds, userIds))
            .willReturn(mockedRoleAssignmentResponse);

        given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
            .willReturn(getRoleAssignments());

        final List<CaseAssignedUserRole> caseAssignedUserRole =
            roleAssignmentService.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

        assertEquals(2, caseAssignedUserRole.size());
        assertThat(caseAssignedUserRole.get(0).getCaseDataId(), is(CASE_ID));
    }

    private RoleAssignments getRoleAssignments() {

        final Instant currentTIme = Instant.now();
        final long oneHour = 3600000;

        final RoleAssignmentAttributes roleAssignmentAttributes =
            RoleAssignmentAttributes.builder().caseId(Optional.of(CASE_ID)).build();

        final List<RoleAssignment> roleAssignments = Arrays.asList(

            RoleAssignment.builder().actorId("actorId").roleType(RoleType.CASE.name())
                .attributes(roleAssignmentAttributes)
                .beginTime(currentTIme.minusMillis(oneHour)).endTime(currentTIme.plusMillis(oneHour)).build(),

            RoleAssignment.builder().actorId("actorId1").roleType(RoleType.CASE.name())
                .attributes(roleAssignmentAttributes)
                .beginTime(currentTIme.minusMillis(oneHour)).endTime(currentTIme.plusMillis(oneHour)).build()
        );
        return RoleAssignments.builder().roleAssignmentsList(roleAssignments).build();
    }

    @Nested
    @DisplayName("deleteRoleAssignments()")
    @SuppressWarnings({"ConstantConditions", "FieldCanBeLocal"})
    class DeleteRoleAssignments {

        private final String role1 = "[ROLE1]";
        private final String role2 = "[ROLE2]";
        @Captor
        private ArgumentCaptor<List<RoleAssignmentQuery>> queryRequestsCaptor;

        @Test
        void shouldDoNothingForNullDeleteRequests() {

            // GIVEN
            List<RoleAssignmentsDeleteRequest> deleteRequests = null;

            // WHEN
            roleAssignmentService.deleteRoleAssignments(deleteRequests);

            // THEN
            verify(roleAssignmentServiceHelper, never()).deleteRoleAssignmentsByQuery(any());
        }

        @Test
        void shouldDoNothingForEmptyDeleteRequests() {

            // GIVEN
            List<RoleAssignmentsDeleteRequest> deleteRequests = new ArrayList<>();

            // WHEN
            roleAssignmentService.deleteRoleAssignments(deleteRequests);

            // THEN
            verify(roleAssignmentServiceHelper, never()).deleteRoleAssignmentsByQuery(any());
        }

        @Test
        void shouldDeleteForSingleDeleteRequests() {

            // GIVEN
            List<RoleAssignmentsDeleteRequest> deleteRequests = List.of(
                RoleAssignmentsDeleteRequest.builder()
                    .caseId(CASE_ID)
                    .userId(USER_ID)
                    .roleNames(List.of(role1)).build()
            );

            // WHEN
            roleAssignmentService.deleteRoleAssignments(deleteRequests);

            // THEN
            // verify data passed to repository has correct values
            verify(roleAssignmentServiceHelper).deleteRoleAssignmentsByQuery(queryRequestsCaptor.capture());
            List<RoleAssignmentQuery> queryRequests = queryRequestsCaptor.getValue();

            assertAll(
                () -> Assertions.assertEquals(deleteRequests.size(), queryRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentQueries(deleteRequests, queryRequests)
            );
        }

        @Test
        void shouldDeleteForMultipleDeleteRequests() {

            // GIVEN
            List<RoleAssignmentsDeleteRequest> deleteRequests = List.of(
                RoleAssignmentsDeleteRequest.builder()
                    .caseId(CASE_ID)
                    .userId(USER_ID)
                    .roleNames(List.of(role1)).build(),

                RoleAssignmentsDeleteRequest.builder()
                    .caseId(CASE_ID)
                    .userId(USER_ID_2) // NB: using different user ID in test data to match assert function's map
                    .roleNames(List.of(role1, role2)).build()
            );

            // WHEN
            roleAssignmentService.deleteRoleAssignments(deleteRequests);

            // THEN
            // verify data passed to repository has correct values
            verify(roleAssignmentServiceHelper).deleteRoleAssignmentsByQuery(queryRequestsCaptor.capture());
            List<RoleAssignmentQuery> queryRequests = queryRequestsCaptor.getValue();

            assertAll(
                () -> Assertions.assertEquals(deleteRequests.size(), queryRequests.size()),
                () -> assertCorrectlyPopulatedRoleAssignmentQueries(deleteRequests, queryRequests)
            );
        }

        private void assertCorrectlyPopulatedRoleAssignmentQueries(
            final List<RoleAssignmentsDeleteRequest> expectedDeleteRequests,
            final List<RoleAssignmentQuery> actualRoleAssignmentQueries
        ) {
            assertNotNull(actualRoleAssignmentQueries);
            Assertions.assertEquals(expectedDeleteRequests.size(), actualRoleAssignmentQueries.size());

            // create map by userID (NB: this relies on the test data using a unique user_id for each query)
            Map<String, RoleAssignmentQuery> queryMapByUser = actualRoleAssignmentQueries.stream()
                .collect(Collectors.toMap(query -> query.getActorId().get(0), query -> query));

            expectedDeleteRequests.forEach(expectedDeleteRequest -> assertAll(
                () -> assertTrue(queryMapByUser.containsKey(expectedDeleteRequest.getUserId())),
                () -> assertCorrectlyPopulatedRoleAssignmentQuery(
                    expectedDeleteRequest,
                    queryMapByUser.get(expectedDeleteRequest.getUserId())
                )
            ));
        }

        private void assertCorrectlyPopulatedRoleAssignmentQuery(
            final RoleAssignmentsDeleteRequest expectedDeleteRequest,
            final RoleAssignmentQuery actualRoleAssignmentQuery
        ) {
            assertNotNull(actualRoleAssignmentQuery);
            assertAll(
                // verify format
                () -> Assertions.assertEquals(1, actualRoleAssignmentQuery.getAttributes().getCaseId().size()),
                () -> Assertions.assertEquals(1, actualRoleAssignmentQuery.getActorId().size()),
                () -> Assertions.assertEquals(1, actualRoleAssignmentQuery.getRoleType().size()),
                () -> Assertions.assertEquals(
                    expectedDeleteRequest.getRoleNames().size(), actualRoleAssignmentQuery.getRoleName().size()
                ),

                // verify data
                () -> Assertions.assertEquals(
                    expectedDeleteRequest.getCaseId(), actualRoleAssignmentQuery.getAttributes().getCaseId().get(0)
                ),
                () -> Assertions.assertEquals(expectedDeleteRequest.getUserId(), actualRoleAssignmentQuery.getActorId()
                    .get(0)),
                () -> Assertions.assertEquals(RoleType.CASE.name(), actualRoleAssignmentQuery.getRoleType().get(0)),
                () -> assertArrayEquals(
                    expectedDeleteRequest.getRoleNames().toArray(), actualRoleAssignmentQuery.getRoleName().toArray()
                )
            );
        }

    }

    @Nested
    @DisplayName("createCaseRoleAssignments()")
    @SuppressWarnings("ConstantConditions")
    class CreateCaseRoleAssignments {

        @Test
        void shouldCreateSingleCaseRoleAssignments() {

            // GIVEN
            CaseDetails caseDetails = createCaseDetails();
            Set<String> roles = Set.of("[ROLE1]");
            boolean replaceExisting = false;

            given(roleAssignmentCategoryService.getRoleCategory(USER_ID)).willReturn(ROLE_CATEGORY_4_USER_1);

            // WHEN
            RoleAssignmentRequestResource roleAssignments = roleAssignmentService.createCaseRoleAssignments(
                caseDetails,
                USER_ID,
                roles.stream().collect(
                    Collectors.toList()),
                replaceExisting
            );

            // THEN
            // verify RoleCategory has been loaded from service
            verify(roleAssignmentCategoryService).getRoleCategory(USER_ID);


            assertAll(
                () -> assertCorrectlyPopulatedRoleAssignment(
                    caseDetails,
                    USER_ID,
                    roles.stream().collect(Collectors.joining()),
                    ROLE_CATEGORY_4_USER_1,
                    roleAssignments
                )
            );
        }

        private void assertCorrectlyPopulatedRoleAssignment(final CaseDetails expectedCaseDetails,
                                                            final String expectedUserId,
                                                            final String expectedRoleName,
                                                            final RoleCategory expectedRoleCategory,
                                                            final RoleAssignmentRequestResource actualRoleAssignment) {

            assertNotNull(actualRoleAssignment);

            actualRoleAssignment.getRequestedRoles().stream()
                .forEach(roleAssignmentResource -> assertAll(
                    () -> Assertions.assertEquals(expectedUserId, roleAssignmentResource.getActorId()),
                    () -> Assertions.assertEquals(expectedRoleName, roleAssignmentResource.getRoleName()),

                    // defaults
                    () -> Assertions.assertEquals(ActorIdType.IDAM.name(), roleAssignmentResource.getActorIdType()),

                    () -> Assertions.assertEquals(
                        Classification.RESTRICTED.name(),
                        roleAssignmentResource.getClassification()
                    ),
                    () -> Assertions.assertEquals(GrantType.SPECIFIC.name(), roleAssignmentResource.getGrantType()),
                    () -> Assertions.assertEquals(
                        expectedRoleCategory.name(),
                        roleAssignmentResource.getRoleCategory()
                    ),
                    () -> assertFalse(roleAssignmentResource.getReadOnly()),
                    () -> assertNotNull(roleAssignmentResource.getBeginTime()),

                    // attributes match case
                    () -> Assertions.assertEquals(
                        Optional.of(expectedCaseDetails.getReferenceAsString()),
                        roleAssignmentResource.getAttributes().getCaseId()
                    ),
                    () -> Assertions.assertEquals(
                        Optional.of(expectedCaseDetails.getJurisdiction()),
                        roleAssignmentResource.getAttributes().getJurisdiction()
                    ),
                    () -> Assertions.assertEquals(
                        Optional.of(expectedCaseDetails.getCaseTypeId()),
                        roleAssignmentResource.getAttributes().getCaseType()
                    )
                ));
        }

        private CaseDetails createCaseDetails() {
            final var caseDetails = CaseDetails.builder()
                .id("123456L")
                .jurisdiction("test-jurisdiction")
                .caseTypeId("case-type-id").build();
            return caseDetails;
        }

    }
}
