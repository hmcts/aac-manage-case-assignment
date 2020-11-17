package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@ApiModel("About To Submit Callback Response")
public class AboutToSubmitCallbackResponse {
    private Map<String, JsonNode> data;
}
