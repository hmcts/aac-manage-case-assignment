package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StartEventResource {
    @JsonProperty("case_details")
    private CaseDetails caseDetails;

    @JsonProperty("event_id")
    private String eventId;

    private String token;
}
