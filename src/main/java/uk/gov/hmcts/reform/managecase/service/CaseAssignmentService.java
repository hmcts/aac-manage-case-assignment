package uk.gov.hmcts.reform.managecase.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import javax.validation.ValidationException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CaseAssignmentService {

    public static final String SOLICITOR_ROLE = "caseworker-%s-solicitor";

    public static final String CASE_NOT_FOUND = "Case ID has to be for an existing case accessible by the invoker.";
    public static final String ASSIGNEE_ROLE_ERROR = "Intended assignee has to be a solicitor "
        + "enabled in the jurisdiction of the case.";
    public static final String ASSIGNEE_ORGA_ERROR = "Intended assignee has to be in the same organisation"
        + " as that of the invoker.";
    public static final String ORGA_POLICY_ERROR = "Case ID has to be one for which a case role is "
        + "represented by the invoker's organisation.";

    private final DataStoreRepository dataStoreRepository;
    private final PrdRepository prdRepository;
    private final JacksonUtils jacksonUtils;

    @Autowired
    public CaseAssignmentService(PrdRepository prdRepository,
                                 DataStoreRepository dataStoreRepository,
                                 JacksonUtils jacksonUtils) {
        this.dataStoreRepository = dataStoreRepository;
        this.prdRepository = prdRepository;
        this.jacksonUtils = jacksonUtils;
    }

    @SuppressWarnings("PMD")
    public String assignCaseAccess(CaseAssignment assignment) {
        CaseDetails caseDetails = getCase(assignment);
        String solicitorRole = String.format(SOLICITOR_ROLE, caseDetails.getJurisdiction());

        FindUsersByOrganisationResponse usersByOrg = prdRepository.findUsersByOrganisation();

        Optional<ProfessionalUser> userOptional = findUserBy(usersByOrg.getUsers(), assignment.getAssigneeId());
        ProfessionalUser assignee = userOptional.orElseThrow(() -> new ValidationException(ASSIGNEE_ORGA_ERROR));

        if (!containsIgnoreCase(assignee.getRoles(), solicitorRole)) {
            throw new ValidationException(ASSIGNEE_ROLE_ERROR);
        }

        OrganisationPolicy invokerPolicy = findInvokerOrgPolicy(caseDetails, usersByOrg.getOrganisationIdentifier());
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

    private Optional<ProfessionalUser> findUserBy(List<ProfessionalUser> users, String assigneeId) {
        return users.stream()
                .filter(user -> assigneeId.equalsIgnoreCase(user.getUserIdentifier()))
                .findFirst();
    }

    private boolean containsIgnoreCase(List<String> list, String value) {
        return list.stream().anyMatch(value::equalsIgnoreCase);
    }
}

