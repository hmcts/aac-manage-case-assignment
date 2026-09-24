package uk.gov.hmcts.reform.managecase.service.casedataaccesscontrol;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory;

import java.util.List;
import java.util.regex.Pattern;

import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.CITIZEN;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.ENFORCEMENT;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.JUDICIAL;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.LEGAL_OPERATIONS;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.RoleCategory.PROFESSIONAL;

import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignments;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentServiceHelper;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentsMapper;

@Service
public class RoleAssignmentCategoryService {

    private static final Pattern PROFESSIONAL_ROLE =
        Pattern.compile(".+-solicitor$|^caseworker-.+-localAuthority$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CITIZEN_ROLE =
        Pattern.compile("^citizen(-.*)?$|^letter-holder$", Pattern.CASE_INSENSITIVE);
    private static final Pattern JUDICIAL_ROLE = Pattern.compile(".+-panelmember$",
        Pattern.CASE_INSENSITIVE);

    private final CaseAssignmentService  caseAssignmentService;
    private final RoleAssignmentServiceHelper roleAssignmentServiceHelper;
    private final RoleAssignmentsMapper roleAssignmentsMapper;

    public RoleAssignmentCategoryService(CaseAssignmentService caseAssignmentService,
                                         RoleAssignmentServiceHelper roleAssignmentServiceHelper,
                                         RoleAssignmentsMapper roleAssignmentsMapper) {
        this.caseAssignmentService = caseAssignmentService;
        this.roleAssignmentsMapper = roleAssignmentsMapper;
        this.roleAssignmentServiceHelper = roleAssignmentServiceHelper;
    }

    public RoleCategory getRoleCategory(String userId) {
        final var idamUserRoles = caseAssignmentService.getAssigneeRoles(userId);
        if (hasProfessionalRole(idamUserRoles)) {
            return PROFESSIONAL;
        } else if (hasCitizenRole(idamUserRoles)) {
            return CITIZEN;
        } else if (hasJudicialRole(idamUserRoles)) {
            return JUDICIAL;
        } else if (hasEnforcementRole(userId)) {
            return ENFORCEMENT;
        } else {
            return LEGAL_OPERATIONS;
        }
    }

    private boolean hasProfessionalRole(List<String> roles) {
        return roles.stream().anyMatch(role -> PROFESSIONAL_ROLE.matcher(role).matches());
    }

    private boolean hasCitizenRole(List<String> roles) {
        return roles.stream().anyMatch(role -> CITIZEN_ROLE.matcher(role).matches());
    }

    private boolean hasJudicialRole(List<String> roles) {
        return roles.stream().anyMatch(role -> JUDICIAL_ROLE.matcher(role).matches());
    }

    private boolean hasEnforcementRole(String userId) {
        RoleAssignments roleAssignments = roleAssignmentsMapper.toRoleAssignments(
            roleAssignmentServiceHelper.getRoleAssignments(userId));

        if (roleAssignments == null || roleAssignments.getRoleAssignmentsList() == null) {
            return false;
        }

        /*
         * Filter for ENFORCEMENT roles with GrantType.STANDARD.
         * The global hmcts-enforcement role has GrantType.BASIC and is therefore not included.
         */
        return roleAssignments.getRoleAssignmentsList().stream()
            .filter(roleAssignment -> roleAssignment.hasGrantType(GrantType.STANDARD))
            .anyMatch(roleAssignment ->
                          ENFORCEMENT.name().equalsIgnoreCase(roleAssignment.getRoleCategory()));
    }


}
