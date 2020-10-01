package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Map;

@Data
public class CaseResource {
    @JsonProperty("id")
    private String reference;

    @JsonProperty("jurisdiction")
    private String jurisdiction;

    @JsonProperty("case_type")
    private String caseType;

    @JsonProperty("state")
    private String state;

    @JsonProperty("data")
    private Map<String, JsonNode> data;
}
