package uk.gov.hmcts.reform.managecase.service;

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
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentQuery;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentRequestResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignments;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentsAddRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("RoleAssignmentService")
@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceTest {

    private static final String CASE_ID = "111111";
    private static final String USER_ID = "user1";
    private static final String USER_ID_2 = "user2";
    private static final RoleCategory ROLE_CATEGORY_4_USER_1 = RoleCategory.PROFESSIONAL;
    private final List<String> caseIds = Arrays.asList("111", "222");
    private final List<String> userIds = Arrays.asList("111", "222");
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

    @Test
    public void shouldGetRoleAssignmentsByCasesAndUsers() {

        given(roleAssignmentServiceHelper.findRoleAssignmentsByCasesAndUsers(caseIds, userIds))
            .willReturn(mockedRoleAssignmentResponse);

        given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
            .willReturn(getRoleAssignments());

        final List<CaseAssignedUserRole> caseAssignedUserRole =
            roleAssignmentService.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

        assertThat(caseAssignedUserRole).hasSize(2);
        assertThat(caseAssignedUserRole.getFirst().getCaseDataId()).isEqualTo(CASE_ID);
    }

    @Test
    void shouldReturnOnlyActiveCaseRoleAssignmentsByCasesAndUsers() {
        RoleAssignment activeCaseRoleAssignment = roleAssignment(
            "actorId", RoleType.CASE.name(),
            "[ROLE1]",
            Optional.of(CASE_ID), Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(60)
        );
        RoleAssignment expiredCaseRoleAssignment = roleAssignment(
            "expiredActorId",
            RoleType.CASE.name(), "[ROLE2]",
            Optional.of("222222"), Instant.now().minusSeconds(120),
            Instant.now().minusSeconds(60)
        );
        RoleAssignment organisationRoleAssignment = roleAssignment(
            "organisationActorId",
            RoleType.ORGANISATION.name(), "[ROLE3]",
            Optional.of("333333"), Instant.now().minusSeconds(60),
            Instant.now().plusSeconds(60)
        );
        RoleAssignments roleAssignments = RoleAssignments.builder()
            .roleAssignmentsList(List.of(
                activeCaseRoleAssignment,
                expiredCaseRoleAssignment,
                organisationRoleAssignment
            ))
            .build();

        given(roleAssignmentServiceHelper.findRoleAssignmentsByCasesAndUsers(caseIds, userIds))
            .willReturn(mockedRoleAssignmentResponse);
        given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse)).willReturn(roleAssignments);

        List<CaseAssignedUserRole> result = roleAssignmentService.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

        assertAll(
            () -> assertThat(result).hasSize(1),
            () -> assertThat(result.getFirst().getCaseDataId()).isEqualTo(CASE_ID),
            () -> assertThat(result.getFirst().getUserId()).isEqualTo("actorId"),
            () -> assertThat(result.getFirst().getCaseRole()).isEqualTo("[ROLE1]")
        );
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

    private RoleAssignment roleAssignment(String actorId,
                                          String roleType,
                                          String roleName,
                                          Optional<String> caseId,
                                          Instant beginTime,
                                          Instant endTime) {
        RoleAssignmentAttributes roleAssignmentAttributes = RoleAssignmentAttributes.builder().caseId(caseId).build();

        return RoleAssignment.builder()
            .actorId(actorId)
            .roleType(roleType)
            .roleName(roleName)
            .attributes(roleAssignmentAttributes)
            .beginTime(beginTime)
            .endTime(endTime)
            .build();
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
                () -> assertThat(queryRequests).hasSameSizeAs(deleteRequests),
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
                () -> assertThat(queryRequests).hasSameSizeAs(deleteRequests),
                () -> assertCorrectlyPopulatedRoleAssignmentQueries(deleteRequests, queryRequests)
            );
        }

        private void assertCorrectlyPopulatedRoleAssignmentQueries(
            final List<RoleAssignmentsDeleteRequest> expectedDeleteRequests,
            final List<RoleAssignmentQuery> actualRoleAssignmentQueries
        ) {
            assertThat(actualRoleAssignmentQueries).isNotNull();
            assertThat(actualRoleAssignmentQueries).hasSameSizeAs(expectedDeleteRequests);

            // create map by userID (NB: this relies on the test data using a unique user_id for each query)
            Map<String, RoleAssignmentQuery> queryMapByUser = actualRoleAssignmentQueries.stream()
                .collect(Collectors.toMap(
                    query -> query.getActorId().getFirst(),
                    query -> query
                ));

            expectedDeleteRequests.forEach(expectedDeleteRequest -> assertAll(
                () -> assertThat(queryMapByUser).containsKey(expectedDeleteRequest.getUserId()),
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
            assertThat(actualRoleAssignmentQuery).isNotNull();
            assertAll(
                // verify format
                () -> assertThat(actualRoleAssignmentQuery.getAttributes().getCaseId()).hasSize(1),
                () -> assertThat(actualRoleAssignmentQuery.getActorId()).hasSize(1),
                () -> assertThat(actualRoleAssignmentQuery.getRoleType()).hasSize(1),
                () -> assertThat(actualRoleAssignmentQuery.getRoleName())
                    .hasSameSizeAs(expectedDeleteRequest.getRoleNames()),

                // verify data
                () -> assertThat(actualRoleAssignmentQuery.getAttributes().getCaseId().getFirst())
                    .isEqualTo(expectedDeleteRequest.getCaseId()),
                () -> assertThat(actualRoleAssignmentQuery.getActorId().getFirst())
                    .isEqualTo(expectedDeleteRequest.getUserId()),
                () -> assertThat(actualRoleAssignmentQuery.getRoleType().getFirst()).isEqualTo(RoleType.CASE.name()),
                () -> assertThat(actualRoleAssignmentQuery.getRoleName())
                    .containsExactlyElementsOf(expectedDeleteRequest.getRoleNames())
            );
        }

    }

    @Nested
    @DisplayName("createCaseRoleAssignments()")
    @SuppressWarnings("ConstantConditions")
    class CreateCaseRoleAssignments {

        @Captor
        private ArgumentCaptor<RoleAssignmentRequestResource> roleAssignmentRequestCaptor;

        @Test
        void shouldCreateSingleCaseRoleAssignments() {

            // GIVEN
            CaseDetails caseDetails = createCaseDetails();
            List<String> roles = List.of("[ROLE1]");
            boolean replaceExisting = false;

            given(roleAssignmentCategoryService.getRoleCategory(USER_ID)).willReturn(ROLE_CATEGORY_4_USER_1);

            // WHEN
            RoleAssignmentRequestResource roleAssignments = roleAssignmentService.createCaseRoleAssignments(
                caseDetails,
                USER_ID,
                roles,
                replaceExisting
            );

            // THEN
            // verify RoleCategory has been loaded from service
            verify(roleAssignmentCategoryService).getRoleCategory(USER_ID);

            assertCorrectlyPopulatedRoleAssignment(
                caseDetails,
                roles.getFirst(),
                roleAssignments
            );
        }

        @Test
        void shouldCreateCaseRoleAssignmentRequestWithMultipleRoles() {
            CaseDetails caseDetails = createCaseDetails();
            List<String> roles = List.of("[ROLE1]", "[ROLE2]");

            given(roleAssignmentCategoryService.getRoleCategory(USER_ID)).willReturn(ROLE_CATEGORY_4_USER_1);

            RoleAssignmentRequestResource result = roleAssignmentService.createCaseRoleAssignments(
                caseDetails,
                USER_ID,
                roles,
                true
            );

            Set<String> roleNames = result.getRequestedRoles().stream()
                .map(RoleAssignmentResource::getRoleName)
                .collect(Collectors.toSet());

            assertAll(
                () -> assertThat(result.getRoleRequest().getAssignerId()).isEqualTo(USER_ID),
                () -> assertThat(result.getRoleRequest().getProcess()).isEqualTo("CCD"),
                () -> assertThat(result.getRoleRequest().getReference())
                    .isEqualTo(caseDetails.getId() + "-" + USER_ID),
                () -> assertThat(result.getRoleRequest().isReplaceExisting()).isTrue(),
                () -> assertThat(result.getRequestedRoles()).hasSize(2),
                () -> assertThat(roleNames).containsExactlyInAnyOrderElementsOf(roles)
            );
        }

        @Test
        void shouldCreateRoleAssignmentForEachAddRequest() {
            CaseDetails firstCaseDetails = createCaseDetails();
            CaseDetails secondCaseDetails = CaseDetails.builder()
                .id("654321L")
                .jurisdiction("test-jurisdiction-two")
                .caseTypeId("case-type-id-two")
                .build();
            List<RoleAssignmentsAddRequest> addRequests = List.of(
                RoleAssignmentsAddRequest.builder()
                    .caseDetails(firstCaseDetails)
                    .roleNames(List.of("[ROLE1]"))
                    .userId(USER_ID)
                    .build(),
                RoleAssignmentsAddRequest.builder()
                    .caseDetails(secondCaseDetails)
                    .roleNames(List.of("[ROLE2]"))
                    .userId(USER_ID_2)
                    .build()
            );

            given(roleAssignmentCategoryService.getRoleCategory(USER_ID)).willReturn(ROLE_CATEGORY_4_USER_1);
            given(roleAssignmentCategoryService.getRoleCategory(USER_ID_2)).willReturn(RoleCategory.LEGAL_OPERATIONS);

            roleAssignmentService.createCaseRoleAssignments(addRequests);

            verify(
                roleAssignmentServiceHelper,
                times(2)
            ).createRoleAssignment(roleAssignmentRequestCaptor.capture());
            List<RoleAssignmentRequestResource> result = roleAssignmentRequestCaptor.getAllValues();

            RoleAssignmentResource roleAssignmentResource1 = result.getFirst().getRequestedRoles().getFirst();
            RoleAssignmentResource roleAssignmentResource2 = result.get(1).getRequestedRoles().getFirst();

            assertAll(
                () -> assertThat(roleAssignmentResource1.getActorId()).isEqualTo(USER_ID),
                () -> assertThat(roleAssignmentResource1.getRoleName()).isEqualTo("[ROLE1]"),
                () -> assertThat(roleAssignmentResource1.getAttributes().getCaseId())
                    .isEqualTo(firstCaseDetails.getReferenceAsString()),
                () -> assertThat(roleAssignmentResource2.getActorId()).isEqualTo(USER_ID_2),
                () -> assertThat(roleAssignmentResource2.getRoleName()).isEqualTo("[ROLE2]"),
                () -> assertThat(roleAssignmentResource2.getAttributes().getCaseId())
                    .isEqualTo(secondCaseDetails.getReferenceAsString())
            );
        }

        @Test
        void shouldDoNothingForNullAddRequests() {
            List<RoleAssignmentsAddRequest> addRequests = null;

            roleAssignmentService.createCaseRoleAssignments(addRequests);

            verify(roleAssignmentCategoryService, never()).getRoleCategory(any());
            verify(roleAssignmentServiceHelper, never()).createRoleAssignment(any());
        }

        @Test
        void shouldDoNothingForEmptyAddRequests() {
            List<RoleAssignmentsAddRequest> addRequests = new ArrayList<>();

            roleAssignmentService.createCaseRoleAssignments(addRequests);

            verify(roleAssignmentCategoryService, never()).getRoleCategory(any());
            verify(roleAssignmentServiceHelper, never()).createRoleAssignment(any());
        }

        @Test
        void shouldCreateCaseRoleAssignmentsForRequest() {
            // GIVEN
            final List<RoleAssignmentsAddRequest> addRequest = new ArrayList<>();
            final var caseDetails = createCaseDetails();
            final var roles = Set.of("[ROLE1]");
            final var roleAssignmentsAddRequest = RoleAssignmentsAddRequest.builder()
                .caseDetails(caseDetails)
                .roleNames(new ArrayList<>(roles))
                .userId(USER_ID).build();

            addRequest.add(roleAssignmentsAddRequest);
            given(roleAssignmentCategoryService.getRoleCategory(USER_ID)).willReturn(ROLE_CATEGORY_4_USER_1);

            // WHEN
            roleAssignmentService.createCaseRoleAssignments(addRequest);

            //THEN
            // verify RoleCategory has been loaded from service
            verify(roleAssignmentCategoryService).getRoleCategory(USER_ID);
        }

        private void assertCorrectlyPopulatedRoleAssignment(final CaseDetails expectedCaseDetails,
                                                            final String expectedRoleName,
                                                            final RoleAssignmentRequestResource actualRoleAssignment) {

            assertThat(actualRoleAssignment).isNotNull();

            actualRoleAssignment.getRequestedRoles()
                .forEach(roleAssignmentResource -> assertAll(
                    () -> assertThat(roleAssignmentResource.getActorId()).isEqualTo(USER_ID),
                    () -> assertThat(roleAssignmentResource.getRoleName()).isEqualTo(expectedRoleName),

                    // defaults
                    () -> assertThat(roleAssignmentResource.getActorIdType()).isEqualTo(ActorIdType.IDAM.name()),

                    () -> assertThat(roleAssignmentResource.getClassification())
                        .isEqualTo(Classification.RESTRICTED.name()),
                    () -> assertThat(roleAssignmentResource.getGrantType()).isEqualTo(GrantType.SPECIFIC.name()),
                    () -> assertThat(roleAssignmentResource.getRoleCategory())
                        .isEqualTo(ROLE_CATEGORY_4_USER_1.name()),
                    () -> assertThat(roleAssignmentResource.getReadOnly()).isFalse(),
                    () -> assertThat(roleAssignmentResource.getBeginTime()).isNotNull(),

                    // attributes match case
                    () -> assertThat(roleAssignmentResource.getAttributes().getCaseId())
                        .isEqualTo(expectedCaseDetails.getReferenceAsString()),
                    () -> assertThat(roleAssignmentResource.getAttributes().getJurisdiction())
                        .isEqualTo(expectedCaseDetails.getJurisdiction()),
                    () -> assertThat(roleAssignmentResource.getAttributes().getCaseType())
                        .isEqualTo(expectedCaseDetails.getCaseTypeId())
                ));
        }

        private CaseDetails createCaseDetails() {
            return CaseDetails.builder()
                .id("123456L")
                .jurisdiction("test-jurisdiction")
                .caseTypeId("case-type-id").build();
        }

    }
}
