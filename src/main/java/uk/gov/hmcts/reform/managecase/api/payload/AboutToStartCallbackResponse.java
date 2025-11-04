package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import uk.gov.hmcts.reform.managecase.client.datastore.model.SecurityClassification;
import uk.gov.hmcts.reform.managecase.client.datastore.model.SignificantItem;

import java.util.List;
import java.util.Map;

/**
 * This class has the same structure as the CallbackResponse in the data-store, because it is a response to a callback.
 */
@Data
@Builder
@Schema(description = "About to Start Callback Response")
public class AboutToStartCallbackResponse {

    @Schema(description = "Case data as defined in case type definition."
        + "See `docs/api/case-data.md` for data structure.")
    private Map<String, JsonNode> data;
    @JsonProperty("data_classification")
    @Schema(description = "Same structure as `data` with classification (`PUBLIC`, `PRIVATE`, `RESTRICTED`) "
        + "as field's value.")
    private Map<String, JsonNode> dataClassification;
    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;
    @JsonProperty("significant_item")
    private SignificantItem significantItem;
    private String state;

    private List<String> errors;
    private List<String> warnings;
}
