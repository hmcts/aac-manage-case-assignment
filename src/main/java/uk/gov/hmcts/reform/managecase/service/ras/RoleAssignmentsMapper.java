package uk.gov.hmcts.reform.managecase.service.ras;

import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignments;

public interface RoleAssignmentsMapper {

    RoleAssignments toRoleAssignments(RoleAssignmentResponse roleAssignmentResponse);
}
