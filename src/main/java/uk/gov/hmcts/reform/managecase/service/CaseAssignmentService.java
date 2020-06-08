package uk.gov.hmcts.reform.managecase.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import javax.validation.ValidationException;
import java.util.List;
import java.util.stream.Stream;

@Service
public class CaseAssignmentService {

    public static final String SOLICITOR_ROLE = "caseworker-%s-solicitor";
    public static final String CASEWORKER_CAA = "caseworker-caa";

    public static final String INVOKER_ROLE_ERROR = "The user is neither a case access administrator"
        + " nor a solicitor with access to the jurisdiction of the case.";

    public static final String ASSIGNEE_ROLE_ERROR = "Intended assignee has to be a solicitor "
        + "enabled in the jurisdiction of the case.";

    public static final String ASSIGNEE_ORGA_ERROR = "Intended assignee has to be in the same organisation"
        + " as that of the invoker.";

    private final DataStoreRepository dataStoreRepository;
    private final PrdRepository prdRepository;
    private final IdamRepository idamRepository;
    private final SecurityUtils securityUtils;

    @Autowired
    public CaseAssignmentService(SecurityUtils securityUtils,
                                 IdamRepository idamRepository,
                                 PrdRepository prdRepository,
                                 DataStoreRepository dataStoreRepository) {
        this.dataStoreRepository = dataStoreRepository;
        this.idamRepository = idamRepository;
        this.prdRepository = prdRepository;
        this.securityUtils = securityUtils;
    }

    @SuppressWarnings("PMD")
    public String assignCaseAccess(CaseAssignment assignment) {
        CaseDetails caseDetails = dataStoreRepository.findCaseBy(assignment.getCaseTypeId(), assignment.getCaseId());
        String solicitorRole = String.format(SOLICITOR_ROLE, caseDetails.getJurisdiction());
        // 1. Validate invoker's roles
        validateInvokerRoles(CASEWORKER_CAA, solicitorRole);
        // 2. validate assignee's roles
        validateAssigneeRoles(assignment.getAssigneeId(), solicitorRole);
        // 3. validate invoker's organisation has assignee
        validateAssigneeOrganisation(assignment);
        // TODO : other validations and invoke final assign data-store api
        // 4.Validate that the case record contains at least 1 organisation policy field with the invoker's organisation
        // 5. Call Grant Case Roles operation of CCD Data Store API
        return "Assigned-Role";
    }

    private void validateAssigneeOrganisation(CaseAssignment assignment) {
        List<ProfessionalUser> users = prdRepository.findUsersByOrganisation();
        if (isAssigneeExists(assignment, users)) {
            throw new ValidationException(ASSIGNEE_ORGA_ERROR);
        }
    }

    private void validateAssigneeRoles(String assigneeId, String inputRole) {
        UserDetails assignee = idamRepository.getUserByUserId(assigneeId);
        if (!assignee.getRoles().contains(inputRole)) {
            throw new ValidationException(ASSIGNEE_ROLE_ERROR);
        }
    }

    private void validateInvokerRoles(String... inputRoles) {
        List<String> invokerRoles = securityUtils.getUserInfo().getRoles();
        if (Stream.of(inputRoles).noneMatch(invokerRoles::contains)) {
            throw new AccessDeniedException(INVOKER_ROLE_ERROR);
        }
    }

    private boolean isAssigneeExists(CaseAssignment assignment, List<ProfessionalUser> users) {
        return users.stream().noneMatch(user -> user.getUserIdentifier().equals(assignment.getAssigneeId()));
    }
}
