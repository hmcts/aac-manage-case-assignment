package uk.gov.hmcts.reform.managecase.service.cau;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CaseAccessOperation {

    private final RoleAssignmentService roleAssignmentService;

    public CaseAccessOperation(RoleAssignmentService roleAssignmentService) {
        this.roleAssignmentService = roleAssignmentService;
    }

    public List<CaseAssignedUserRole> findCaseUserRoles(List<Long> caseReferences, List<String> userIds) {
        final var caseIds = caseReferences.stream().map(String::valueOf).collect(Collectors.toList());
        return roleAssignmentService.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);
    }
}
