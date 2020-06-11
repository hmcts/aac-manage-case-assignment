package uk.gov.hmcts.reform.managecase.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import javax.validation.ValidationException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CaseAssignmentService {

    public static final String SOLICITOR_ROLE = "caseworker-%s-solicitor";
    public static final String CASEWORKER_CAA = "caseworker-caa";

    public static final String CASE_NOT_FOUND = "Case ID has to be for an existing case accessible by the invoker.";
    public static final String INVOKER_ROLE_ERROR = "The user is neither a case access administrator"
        + " nor a solicitor with access to the jurisdiction of the case.";
    public static final String ASSIGNEE_ROLE_ERROR = "Intended assignee has to be a solicitor "
        + "enabled in the jurisdiction of the case.";
    public static final String ASSIGNEE_ORGA_ERROR = "Intended assignee has to be in the same organisation"
        + " as that of the invoker.";
    public static final String ORGA_POLICY_ERROR = "Case ID has to be one for which a case role is "
        + "represented by the invoker's organisation.";

    private final DataStoreRepository dataStoreRepository;
    private final PrdRepository prdRepository;
    private final SecurityUtils securityUtils;
    private final JacksonUtils jacksonUtils;

    @Autowired
    public CaseAssignmentService(SecurityUtils securityUtils,
                                 PrdRepository prdRepository,
                                 DataStoreRepository dataStoreRepository,
                                 JacksonUtils jacksonUtils) {
        this.dataStoreRepository = dataStoreRepository;
        this.prdRepository = prdRepository;
        this.securityUtils = securityUtils;
        this.jacksonUtils = jacksonUtils;
    }

    @SuppressWarnings("PMD")
    public String assignCaseAccess(CaseAssignment assignment) {
        CaseDetails caseDetails = getCase(assignment);
        String solicitorRole = String.format(SOLICITOR_ROLE, caseDetails.getJurisdiction());

        // There is no generic pattern for these validations - still worth to extract to a Validator class??
        validateInvokerRoles(CASEWORKER_CAA, solicitorRole);

        Optional<ProfessionalUser> userOptional = prdRepository.findUserBy(assignment.getAssigneeId());
        ProfessionalUser assignee = userOptional.orElseThrow(() -> new ValidationException(ASSIGNEE_ORGA_ERROR));

        if (!assignee.getRoles().contains(solicitorRole)) {
            throw new ValidationException(ASSIGNEE_ROLE_ERROR);
        }

        OrganisationPolicy invokerPolicy = findInvokerOrgPolicy(caseDetails, assignee.getOrganisationIdentifier());
        dataStoreRepository.assignCase(
            assignment.getCaseId(), invokerPolicy.getOrgPolicyCaseAssignedRole(), assignment.getAssigneeId()
        );
        return invokerPolicy.getOrgPolicyCaseAssignedRole();
    }

    private CaseDetails getCase(CaseAssignment input) {
        Optional<CaseDetails> caseOptional = dataStoreRepository.findCaseBy(input.getCaseTypeId(), input.getCaseId());
        return caseOptional.orElseThrow(() -> new ValidationException(CASE_NOT_FOUND));
    }

    private OrganisationPolicy findInvokerOrgPolicy(CaseDetails caseDetails, String organisation) {
        List<OrganisationPolicy> policies = findPolicies(caseDetails);
        return policies.stream()
            .filter(policy -> organisation.equalsIgnoreCase(policy.getOrganisation().getOrganisationID()))
            .findFirst()
            .orElseThrow(() -> new ValidationException(ORGA_POLICY_ERROR));
    }

    private List<OrganisationPolicy> findPolicies(CaseDetails caseDetails) {
        List<JsonNode> policyNodes = caseDetails.findOrganisationPolicyNodes();
        return policyNodes.stream()
            .map(node -> jacksonUtils.convertValue(node, OrganisationPolicy.class))
            .collect(Collectors.toList());
    }

    private void validateInvokerRoles(String... inputRoles) {
        List<String> invokerRoles = securityUtils.getUserInfo().getRoles();
        if (Stream.of(inputRoles).noneMatch(invokerRoles::contains)) {
            throw new AccessDeniedException(INVOKER_ROLE_ERROR);
        }
    }

}
