package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.managecase.client.datastore.model.SecurityClassification;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseDetails {

    protected static final String ORGANISATION_TO_ADD = "OrganisationToAdd";
    protected static final String ORGANISATION_TO_REMOVE = "OrganisationToRemove";
    protected static final String ORGANISATION_CASE_ROLE_ID = "CaseRoleId";
    protected static final String ORGANISATION_REQUEST_TIMESTAMP = "RequestTimestamp";
    public static final String ORG_POLICY_CASE_ASSIGNED_ROLE = "OrgPolicyCaseAssignedRole";
    public static final String ORG_POLICY_REFERENCE = "OrgPolicyReference";
    public static final String ORG_ID = "OrganisationID";
    public static final String ORG_NAME = "OrganisationName";

    private String reference;
    private String jurisdiction;
    private String state;
    @JsonProperty("case_type_id")
    private String caseTypeId;
    @JsonProperty("case_data")
    private Map<String, JsonNode> data;
    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;
    @JsonProperty("data_classification")
    @ApiModelProperty("Same structure as `case_data` with classification (`PUBLIC`, `PRIVATE`, `RESTRICTED`) "
        + "as field's value.")
    private Map<String, JsonNode> dataClassification;

    public Optional<String> findChangeOrganisationRequestFieldName() {
        Optional<JsonNode> first = getData().values().stream()
            .map(node -> node.findParents(ORGANISATION_TO_ADD))
            .flatMap(List::stream)
            .map(node -> node.findParents(ORGANISATION_TO_REMOVE))
            .flatMap(List::stream)
            .map(node -> node.findParents(ORGANISATION_CASE_ROLE_ID))
            .flatMap(List::stream)
            .map(node -> node.findParents(ORGANISATION_REQUEST_TIMESTAMP))
            .flatMap(List::stream)
            .findFirst();

        if (first.isPresent()) {
            for (Map.Entry<String, JsonNode> entry : getData().entrySet()) {
                if (entry.getValue().equals(first.get())) {
                    return Optional.of(entry.getKey());
                }
            }
        }
        return Optional.empty();
    }

    public List<JsonNode> findOrganisationPolicyNodes() {
        return getData().values().stream()
            .map(node -> node.findParents(ORG_POLICY_CASE_ASSIGNED_ROLE))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

}
