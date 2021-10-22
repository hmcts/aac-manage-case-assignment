package uk.gov.hmcts.reform.managecase.service.ras;

import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentQuery;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentRequestResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentRequestResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;

import java.util.List;

public interface RoleAssignmentServiceHelper {

    void deleteRoleAssignmentsByQuery(List<RoleAssignmentQuery> queryRequests);

    RoleAssignmentRequestResponse createRoleAssignment(RoleAssignmentRequestResource assignmentRequest);

    RoleAssignmentResponse findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds);
}
