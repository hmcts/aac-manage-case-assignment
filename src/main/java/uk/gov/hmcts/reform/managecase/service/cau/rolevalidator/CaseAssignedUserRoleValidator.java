package uk.gov.hmcts.reform.managecase.service.cau.rolevalidator;

import java.util.List;

public interface CaseAssignedUserRoleValidator {

    boolean canAccessUserCaseRoles(List<String> userIds);
}
