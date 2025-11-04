package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Apply Notice of Change Decision Response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplyNoCDecisionResponse {

    @Schema(description = "Case data")
    private Map<String, JsonNode> data;

    @Schema(description = "Errors")
    private List<String> errors;
}
