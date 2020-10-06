package uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CommonViewItem;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORG_POLICY_CASE_ASSIGNED_ROLE;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.CaseFieldPathUtils.getNestedCaseFieldByPath;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class SearchResultViewItem implements CommonViewItem {

    @JsonProperty("case_id")
    private String caseId;
    private Map<String, JsonNode> fields;
    @JsonProperty("fields_formatted")
    private Map<String, JsonNode> fieldsFormatted;

    public List<JsonNode> findOrganisationPolicyNodes() {
        return getFields().values().stream()
            .map(node -> node.findParents(ORG_POLICY_CASE_ASSIGNED_ROLE))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    public String getFieldValue(String fieldId) {
        JsonNode fieldNode = getNestedCaseFieldByPath(fields, fieldId);
        return fieldNode == null || fieldNode.isNull() ? null : fieldNode.asText();
    }

    public List<OrganisationPolicy> findPolicies() {
        List<JsonNode> policyNodes = findOrganisationPolicyNodes();
        return policyNodes.stream()
            .map(node -> OrganisationPolicy.builder()
                .orgPolicyCaseAssignedRole(node.get("OrgPolicyCaseAssignedRole").asText())
                .orgPolicyReference(node.get("OrgPolicyReference").asText()).build())
            .collect(Collectors.toList());
    }

    public Optional<OrganisationPolicy> findOrganisationPolicyForRole(String caseRoleId) {
        return findPolicies().stream()
            .filter(policy -> policy.getOrgPolicyCaseAssignedRole().equals(caseRoleId))
            .findFirst();
    }
}
