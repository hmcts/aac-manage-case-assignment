package uk.gov.hmcts.reform.managecase.client.datastore;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class CaseDataContent {

    private Event event;

    private Map<String, JsonNode> data;

    @JsonProperty("event_token")
    private String token;

    //@JsonProperty("on_behalf_of_token")
    //private String onBehalfOfUserToken;
}
