package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseResource {
    public static final String ORGANISATION_TO_ADD = "OrganisationToAdd";
    public static final String ORG_POLICY_CASE_ASSIGNED_ROLE = "OrgPolicyCaseAssignedRole";

    @JsonProperty("id")
    private String reference;

    @JsonProperty("jurisdiction")
    private String jurisdiction;

    @JsonProperty("case_type")
    private String caseType;

    @JsonProperty("state")
    private String state;

    @JsonProperty("data")
    private Map<String, JsonNode> data;

    public Optional<JsonNode> findChangeOrganisationRequestNode() {
        return getData().values().stream()
            .map(node -> node.findParents(ORGANISATION_TO_ADD))
            .flatMap(List::stream)
            .findFirst();
    }

    public List<JsonNode> findOrganisationPolicyNodes() {
        return getData().values().stream()
            .map(node -> node.findParents(ORG_POLICY_CASE_ASSIGNED_ROLE))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
}
