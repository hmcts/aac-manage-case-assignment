package uk.gov.hmcts.reform.managecase.service.ras;

import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;

import java.util.List;

public interface RoleAssignmentServiceHelper {

    RoleAssignmentResponse findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds);
}
