package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.LuhnCheck;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallbackCaseDetails {

    public static final String CASE_ROLE_ID  = "CaseRoleId";
    public static final String ORG_POLICY_CASE_ASSIGNED_ROLE = "OrgPolicyCaseAssignedRole";

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

    public List<JsonNode> findOrganisationPolicyNodes() {
        return getData().values().stream()
            .map(node -> node.findParents(ORG_POLICY_CASE_ASSIGNED_ROLE))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    public Optional<JsonNode> findChangeOrganisationRequestNode() {
        return getData().values().stream()
            .filter(node -> node.findParent(CASE_ROLE_ID) != null)
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
