package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("Verify Notice of Change Answers Response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplyNoCDecisionResponse {

    @ApiModelProperty(value = "Case data", required = false)
    private Map<String, JsonNode> data;

    @ApiModelProperty(value = "Errors", required = false)
    private List<String> errors;
}
