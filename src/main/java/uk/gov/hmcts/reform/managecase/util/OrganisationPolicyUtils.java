package uk.gov.hmcts.reform.managecase.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class OrganisationPolicyUtils {

    private final JacksonUtils jacksonUtils;

    @Autowired
    public OrganisationPolicyUtils(JacksonUtils jacksonUtils) {
        this.jacksonUtils = jacksonUtils;
    }

    public List<OrganisationPolicy> findPolicies(CaseDetails caseDetails) {
        List<JsonNode> policyNodes = caseDetails.findOrganisationPolicyNodes();
        return policyNodes.stream()
            .map(node -> jacksonUtils.convertValue(node, OrganisationPolicy.class))
            .collect(toList());
    }

    public List<String> findRolesForOrg(List<OrganisationPolicy> policies, String orgId) {
        return policies.stream()
            .filter(policy -> policy.getOrganisation() != null && orgId.equalsIgnoreCase(policy
                                                                                             .getOrganisation()
                                                                                             .getOrganisationID()))
            .map(OrganisationPolicy::getOrgPolicyCaseAssignedRole)
            .collect(toList());
    }

    public boolean checkIfPolicyHasOrganisationAssigned(OrganisationPolicy policy) {
        return policy != null
            && policy.getOrganisation() != null
            && StringUtils.isNotEmpty(policy.getOrganisation().getOrganisationID());
    }

}
