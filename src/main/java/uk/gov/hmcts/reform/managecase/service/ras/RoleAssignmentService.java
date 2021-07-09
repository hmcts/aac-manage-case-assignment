package uk.gov.hmcts.reform.managecase.service.ras;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignment;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentAttributes;
import uk.gov.hmcts.reform.managecase.api.payload.RoleType;
import uk.gov.hmcts.reform.managecase.repository.RoleAssignmentRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleAssignmentService {

    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleAssignmentsMapper roleAssignmentsMapper;

    @Autowired
    public RoleAssignmentService(RoleAssignmentRepository roleAssignmentRepository,
                                 RoleAssignmentsMapper roleAssignmentsMapper) {
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.roleAssignmentsMapper = roleAssignmentsMapper;
    }

    public List<CaseAssignedUserRole> findRoleAssignmentsByCasesAndUsers(List<String> caseIds, List<String> userIds) {
        final var roleAssignmentResponse =
            roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

        final var roleAssignments = roleAssignmentsMapper.toRoleAssignments(roleAssignmentResponse);
        var caseIdError = new RuntimeException(RoleAssignmentAttributes.ATTRIBUTE_NOT_DEFINED);
        return roleAssignments.getRoleAssignmentsList().stream()
            .filter(roleAssignment -> isValidRoleAssignment(roleAssignment))
            .map(roleAssignment ->
                     new CaseAssignedUserRole(
                         roleAssignment.getAttributes().getCaseId().orElseThrow(() -> caseIdError),
                         roleAssignment.getActorId(),
                         roleAssignment.getRoleName()
                     )
            )
            .collect(Collectors.toList());
    }

    private boolean isValidRoleAssignment(RoleAssignment roleAssignment) {
        final boolean isCaseRoleType = roleAssignment.getRoleType().equals(RoleType.CASE.name());
        return roleAssignment.isNotExpiredRoleAssignment() && isCaseRoleType;
    }
}
