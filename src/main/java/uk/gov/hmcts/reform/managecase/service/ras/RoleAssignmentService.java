package uk.gov.hmcts.reform.managecase.service.ras;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.ActorIdType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.Classification;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleType;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignment;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentAttributes;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentAttributesResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentQuery;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentRequestResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentsAddRequest;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentsDeleteRequest;
import uk.gov.hmcts.reform.managecase.api.payload.RoleRequestResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.service.casedataaccesscontrol.RoleAssignmentCategoryService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoleAssignmentService {

    private final RoleAssignmentServiceHelper roleAssignmentServiceHelper;
    private final RoleAssignmentsMapper roleAssignmentsMapper;
    private final RoleAssignmentCategoryService roleAssignmentCategoryService;
    private static final String DEFAULT_PROCESS = "CCD";

    @Autowired
    public RoleAssignmentService(RoleAssignmentServiceHelper roleAssignmentServiceHelper,
                                 RoleAssignmentsMapper roleAssignmentsMapper,
                                 RoleAssignmentCategoryService roleAssignmentCategoryService) {
        this.roleAssignmentServiceHelper = roleAssignmentServiceHelper;
        this.roleAssignmentsMapper = roleAssignmentsMapper;
        this.roleAssignmentCategoryService = roleAssignmentCategoryService;
    }

    public void deleteRoleAssignments(List<RoleAssignmentsDeleteRequest> deleteRequests) {
        if (deleteRequests != null && !deleteRequests.isEmpty()) {
            List<RoleAssignmentQuery> queryRequests = deleteRequests.stream()
                .map(request -> new RoleAssignmentQuery(
                    request.getCaseId(),
                    request.getUserId(),
                    request.getRoleNames())
                )
                .collect(Collectors.toList());

            roleAssignmentServiceHelper.deleteRoleAssignmentsByQuery(queryRequests);
        }
    }

    public void createCaseRoleAssignments(final List<RoleAssignmentsAddRequest> addRequest) {

        if (addRequest != null && !addRequest.isEmpty()) {
            final var queryRequests = addRequest.stream()
                .map(request -> createCaseRoleAssignments(
                         request.getCaseDetails(),
                         request.getUserId(),
                         request.getRoleNames(),
                         false
                     )
                ).collect(Collectors.toList());

            queryRequests.stream().map(assignmentRequest ->
                                           roleAssignmentServiceHelper.createRoleAssignment(assignmentRequest)
            );
        }
    }

    public RoleAssignmentRequestResource createCaseRoleAssignments(final CaseDetails caseDetails,
                                                                   final String userId,
                                                                   final List<String> roles,
                                                                   final boolean replaceExisting
    ) {

        final var roleCategory = roleAssignmentCategoryService.getRoleCategory(userId);
        log.debug("user: {} has roleCategory: {}", userId, roleCategory);

        final var roleRequest = RoleRequestResource.builder()
            .assignerId(userId)
            .process(DEFAULT_PROCESS)
            .reference(createRoleRequestReference(caseDetails, userId))
            .replaceExisting(replaceExisting)
            .build();

        final var requestedRoles = roles.stream()
            .map(roleName -> RoleAssignmentResource.builder()
                .actorIdType(ActorIdType.IDAM.name())
                .actorId(userId)
                .roleType(RoleType.CASE.name())
                .roleName(roleName)
                .classification(Classification.RESTRICTED.name())
                .grantType(GrantType.SPECIFIC.name())
                .roleCategory(roleCategory.name())
                .readOnly(false)
                .beginTime(Instant.now())
                .attributes(RoleAssignmentAttributesResource.builder()
                                .jurisdiction(Optional.of(caseDetails.getJurisdiction()))
                                .caseType(Optional.of(caseDetails.getCaseTypeId()))
                                .caseId(Optional.of(caseDetails.getReferenceAsString()))
                                .build())
                .build())
            .collect(Collectors.toList());

        final var assignmentRequest = RoleAssignmentRequestResource.builder()
            .roleRequest(roleRequest)
            .requestedRoles(requestedRoles)
            .build();

        return assignmentRequest;
    }

    private String createRoleRequestReference(final CaseDetails caseDetails, final String userId) {
        return caseDetails.getId() + "-" + userId;
    }

    public List<CaseAssignedUserRole> findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds) {
        final var roleAssignmentResponse =
            roleAssignmentServiceHelper.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

        final var roleAssignments = roleAssignmentsMapper.toRoleAssignments(roleAssignmentResponse);
        var caseIdError = new RuntimeException(RoleAssignmentAttributes.ATTRIBUTE_NOT_DEFINED);
        return roleAssignments.getRoleAssignmentsList().stream()
            .filter(roleAssignment -> isValidRoleAssignment(roleAssignment))
            .map(roleAssignment ->
                     new CaseAssignedUserRole(
                         roleAssignment.getAttributes().getCaseId().orElseThrow(() -> caseIdError),
                         roleAssignment.getActorId(),
                         roleAssignment.getRoleName()
                     )
            )
            .collect(Collectors.toList());
    }

    private boolean isValidRoleAssignment(RoleAssignment roleAssignment) {
        final boolean isCaseRoleType = roleAssignment.getRoleType().equals(RoleType.CASE.name());
        return roleAssignment.isNotExpiredRoleAssignment() && isCaseRoleType;
    }
}
