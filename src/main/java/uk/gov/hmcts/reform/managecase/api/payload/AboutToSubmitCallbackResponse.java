package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@Schema(description = "About To Submit Callback Response")
public class AboutToSubmitCallbackResponse {
    private Map<String, JsonNode> data;
}
