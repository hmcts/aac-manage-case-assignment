package uk.gov.hmcts.reform.managecase.service.cau;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseRoleAccessException;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.service.cau.rolevalidator.CaseAssignedUserRoleValidator;

import java.util.List;

import static uk.gov.hmcts.reform.managecase.api.errorhandling.AuthError.OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED;

@Service
@Qualifier("authorised")
public class AuthorisedCaseAssignedUserRolesOperation implements CaseAssignedUserRolesOperation {

    private final CaseAssignedUserRolesOperation cauRolesOperation;
    private CaseAssignedUserRoleValidator cauRoleValidator;

    @Autowired
    public AuthorisedCaseAssignedUserRolesOperation(final @Qualifier("default")
                                                        CaseAssignedUserRolesOperation cauRolesOperation,
                                                    @Qualifier("default")
                                                    final CaseAssignedUserRoleValidator cauRoleValidator) {
        this.cauRolesOperation = cauRolesOperation;
        this.cauRoleValidator = cauRoleValidator;
    }

    public List<CaseAssignedUserRole> findCaseUserRoles(List<Long> caseIds, List<String> userIds) {
        if (this.cauRoleValidator.canAccessUserCaseRoles(userIds)) {
            return this.cauRolesOperation.findCaseUserRoles(caseIds, userIds);
        }
        throw new CaseRoleAccessException(OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED);
    }

}
