package uk.gov.hmcts.reform.managecase.repository;

import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;

import java.util.List;

public interface RoleAssignmentRepository {

    RoleAssignmentResponse findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds);
}
