package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.LuhnCheck;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;
import uk.gov.hmcts.reform.managecase.client.datastore.model.SecurityClassification;

import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.managecase.client.datastore.model.CaseFieldPathUtils.getNestedCaseFieldByPath;

@Getter
@EqualsAndHashCode
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CaseDetails {

    public static final String ORGANISATION_TO_ADD = "OrganisationToAdd";
    public static final String CREATED_BY = "CreatedBy";
    public static final String LAST_NOC_REQUESTED_BY = "LastNoCRequestedBy";
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

    @JsonProperty("version")
    private Integer version;

    @JsonProperty("_links")
    private JsonNode links;

    @Setter
    @JsonProperty("jurisdiction")
    private String jurisdiction;

    @JsonProperty("state")
    private String state;

    @JsonProperty("case_type_id")
    @JsonAlias("case_type") // alias to match with data-store V2 external api GetCase
    private String caseTypeId;

    @JsonProperty("case_data")
    @JsonAlias("data")
    @Setter
    private Map<String, JsonNode> data;
    @JsonProperty("created_date")
    @JsonAlias("created_on")
    private LocalDateTime createdDate;
    @JsonProperty("last_modified")
    @JsonAlias("last_modified_on")
    private LocalDateTime lastModified;
    @JsonProperty("last_state_modified_date")
    @JsonAlias("last_state_modified_on")
    private LocalDateTime lastStateModifiedDate;
    @JsonProperty("created_by")
    private String createdBy;
    @JsonProperty("last_modified_by")
    private String lastModifiedBy;
    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;
    @JsonProperty("data_classification")
    @Schema(description = "Same structure as `case_data` with classification (`PUBLIC`, `PRIVATE`, `RESTRICTED`) "
        + "as field's value.")
    private Map<String, JsonNode> dataClassification;

    @JsonProperty("callback_response_status")
    private String callbackResponseStatus;
    @JsonProperty("after_submit_callback_response")
    private JsonNode afterSubmitCallbackResponse;
    @JsonProperty("callback_response_status_code")
    private Integer callbackResponseStatusCode;
    @JsonProperty("delete_draft_response_status")
    private String deleteDraftResponseStatus;
    @JsonProperty("delete_draft_response_status_code")
    private Integer deleteDraftResponseStatusCode;
    @JsonProperty("supplementary_data")
    private JsonNode supplementaryData;

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
            .filter(node -> node.get(ORG_POLICY_CASE_ASSIGNED_ROLE).asText().equalsIgnoreCase(caseRoleId))
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

    public List<JsonNode> findCorNodes() {
        return getData().values().stream()
            .map(node -> node.findParents(ORGANISATION_TO_ADD))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    public boolean hasCaseRoleId() {
        return getData().values().stream()
            .map(node -> node.findPath(CASE_ROLE_ID))
            .anyMatch(node -> !node.isMissingNode() && !node.isNull());
    }

    public String getFieldValue(String fieldId) {
        JsonNode fieldNode = getNestedCaseFieldByPath(data, fieldId);
        return fieldNode == null || fieldNode.isNull() ? null : fieldNode.asText();
    }

    @JsonIgnore
    public Long getReferenceAsLong() {
        return Long.parseLong(id);
    }

    @JsonIgnore
    public String getReferenceAsString() {
        return id != null ? id.toString() : null;
    }
}
