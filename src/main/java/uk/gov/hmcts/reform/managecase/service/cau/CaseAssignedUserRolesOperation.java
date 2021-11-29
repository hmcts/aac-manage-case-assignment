package uk.gov.hmcts.reform.managecase.service.cau;

import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRoleWithOrganisation;

import java.util.List;

public interface CaseAssignedUserRolesOperation {

    void addCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles);

    List<CaseAssignedUserRole> findCaseUserRoles(List<Long> caseIds, List<String> userIds);

    void removeCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles);

}
