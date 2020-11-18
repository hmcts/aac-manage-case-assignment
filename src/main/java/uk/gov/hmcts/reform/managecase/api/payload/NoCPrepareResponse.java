package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
@ApiModel("Prepare NoC Response")
public class NoCPrepareResponse {

    @JsonProperty("data")
    private Map<String, JsonNode> data;
}
