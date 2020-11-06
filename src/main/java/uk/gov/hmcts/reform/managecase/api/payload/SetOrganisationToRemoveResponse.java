package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@ApiModel("Set Organisation To Remove Response")
public class SetOrganisationToRemoveResponse {
    private Map<String, JsonNode> data;
}
