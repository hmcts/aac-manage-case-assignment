package uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CommonViewItem;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORG_ID;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORG_NAME;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORG_POLICY_CASE_ASSIGNED_ROLE;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORG_POLICY_REFERENCE;

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

    public List<OrganisationPolicy> findPolicies() {
        List<JsonNode> policyNodes = findOrganisationPolicyNodes();
        return policyNodes.stream()
            .map(node -> {
                JsonNode org = node.get("Organisation");
                return OrganisationPolicy.builder()
                    .organisation(Organisation.builder()
                        .organisationID(org.get(ORG_ID).textValue())
                        .organisationName(org.get(ORG_NAME).textValue())
                        .build())
                    .orgPolicyCaseAssignedRole(node.get(ORG_POLICY_CASE_ASSIGNED_ROLE).textValue())
                    .orgPolicyReference(node.get(ORG_POLICY_REFERENCE).textValue()).build();
            }).collect(Collectors.toList());
    }
}
