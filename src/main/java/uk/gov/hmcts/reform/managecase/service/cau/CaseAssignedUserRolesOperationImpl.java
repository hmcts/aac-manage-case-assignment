package uk.gov.hmcts.reform.managecase.service.cau;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;

import java.util.List;

@Service
@Qualifier("default")
public class CaseAssignedUserRolesOperationImpl implements CaseAssignedUserRolesOperation  {

    private final CaseAccessOperation caseAccessOperation;

    @Autowired
    public CaseAssignedUserRolesOperationImpl(CaseAccessOperation caseAccessOperation) {
        this.caseAccessOperation = caseAccessOperation;
    }

    public List<CaseAssignedUserRole> findCaseUserRoles(List<Long> caseIds, List<String> userIds) {
        return this.caseAccessOperation.findCaseUserRoles(caseIds, userIds);
    }
}
