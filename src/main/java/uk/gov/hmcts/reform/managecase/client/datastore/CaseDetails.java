package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.LuhnCheck;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;
import uk.gov.hmcts.reform.managecase.client.datastore.model.SecurityClassification;

import javax.validation.ValidationException;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseDetails {

    public static final String ORGANISATION_TO_ADD = "OrganisationToAdd";
    public static final String ORGANISATION_TO_REMOVE = "OrganisationToRemove";
    public static final String CASE_ROLE_ID = "CaseRoleId";
    public static final String ORGANISATION_REQUEST_TIMESTAMP = "RequestTimestamp";
    public static final String ORG_POLICY_CASE_ASSIGNED_ROLE = "OrgPolicyCaseAssignedRole";
    public static final String ORGANISATION = "Organisation";
    public static final String APPROVAL_STATUS = "ApprovalStatus";
    public static final String ORG_POLICY_REFERENCE = "OrgPolicyReference";
    public static final String ORG_ID = "OrganisationID";
    public static final String ORG_NAME = "OrganisationName";
    public static final String PREVIOUS_ORGANISATIONS = "PreviousOrganisations";

    @JsonProperty("id")
    @JsonAlias("reference") // alias to match with data-store elasticSearch api response
    @NotEmpty(message = ValidationError.CASE_ID_EMPTY)
    @Size(min = 16, max = 16, message = ValidationError.CASE_ID_INVALID_LENGTH)
    @LuhnCheck(message = ValidationError.CASE_ID_INVALID, ignoreNonDigitCharacters = false)
    private String id;

    private String jurisdiction;

    private String state;

    @JsonProperty("case_type_id")
    private String caseTypeId;

    @JsonProperty("case_data")
    private Map<String, JsonNode> data;
    @JsonProperty("created_date")
    private LocalDateTime createdDate;
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
            .findAny();

        return first.flatMap(jsonNode -> getData().entrySet().stream()
            .filter(entry -> entry.getValue().equals(jsonNode))
            .map(Map.Entry::getKey)
            .findAny());
    }

    public List<JsonNode> findOrganisationPolicyNodes() {
        return getData().values().stream()
            .map(node -> node.findParents(ORG_POLICY_CASE_ASSIGNED_ROLE))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    public JsonNode findOrganisationPolicyNodeForCaseRole(String caseRoleId) {
        return findOrganisationPolicyNodes().stream()
            .filter(node -> node.get(ORG_POLICY_CASE_ASSIGNED_ROLE).asText().equals(caseRoleId))
            .reduce((a, b) -> {
                throw new ValidationException(String.format("More than one Organisation Policy with "
                    + "case role ID '%s' exists on case", caseRoleId));
            })
            .orElseThrow(() -> new ValidationException(String.format("No Organisation Policy found with "
                + "case role ID '%s'", caseRoleId)));
    }

    public Optional<JsonNode> findChangeOrganisationRequestNode() {
        return getData().values().stream()
            .map(node -> node.findParents(ORGANISATION_TO_ADD))
            .flatMap(List::stream)
            .findFirst();
    }

    public Optional<JsonNode> findCorNodeWithApprovalStatus() {
        return getData().values().stream()
            .filter(node -> node.findParent(CASE_ROLE_ID) != null)
            .filter(node -> node.hasNonNull(APPROVAL_STATUS))
            .filter(node -> node.hasNonNull(ORGANISATION_TO_ADD) || node.hasNonNull(ORGANISATION_TO_REMOVE))
            .findFirst();
    }

    public String getKeyFromDataWithValue(JsonNode value) {
        for (String key : data.keySet()) {
            if (data.get(key).equals(value)) {
                return key;
            }
        }
        return null;
    }
}
