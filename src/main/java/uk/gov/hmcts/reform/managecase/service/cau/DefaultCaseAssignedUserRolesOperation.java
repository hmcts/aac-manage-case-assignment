package uk.gov.hmcts.reform.managecase.service.cau;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRoleWithOrganisation;

import java.util.List;

@Service
@Qualifier("default")
public class DefaultCaseAssignedUserRolesOperation implements CaseAssignedUserRolesOperation  {

    private final CaseAccessOperation caseAccessOperation;

    @Autowired
    public DefaultCaseAssignedUserRolesOperation(CaseAccessOperation caseAccessOperation) {
        this.caseAccessOperation = caseAccessOperation;
    }

    @Override
    public void removeCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) {
        this.caseAccessOperation.removeCaseUserRoles(caseUserRoles);
    }

    public List<CaseAssignedUserRole> findCaseUserRoles(List<Long> caseIds, List<String> userIds) {
        return this.caseAccessOperation.findCaseUserRoles(caseIds, userIds);
    }

    public void addCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) {
        this.caseAccessOperation.addCaseUserRoles(caseUserRoles);
    }
}
