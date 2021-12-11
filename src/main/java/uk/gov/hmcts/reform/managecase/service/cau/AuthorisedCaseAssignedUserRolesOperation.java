package uk.gov.hmcts.reform.managecase.service.cau;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseRoleAccessException;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRoleWithOrganisation;
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

    @Override
    public void removeCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) {
        this.cauRolesOperation.removeCaseUserRoles(caseUserRoles);
    }

    public List<CaseAssignedUserRole> findCaseUserRoles(List<Long> caseIds, List<String> userIds) {
        if (this.cauRoleValidator.canAccessUserCaseRoles(userIds)) {
            return this.cauRolesOperation.findCaseUserRoles(caseIds, userIds);
        }
        throw new CaseRoleAccessException(OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED);
    }

    public void addCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) {
        // NB: Although there are no user based authorisation steps performed here ...
        // ... there are additional s2s authorisation steps performed in the controller.
        this.cauRolesOperation.addCaseUserRoles(caseUserRoles);
    }

}
