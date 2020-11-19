package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("Prepare NoC Request")
public class NoCPrepareRequest {

    @JsonProperty("case_details")
    private CaseDetails caseDetails;

    @JsonProperty("case_details_before")
    private CaseDetails caseDetailsBefore;

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("ignore_warning")
    private Boolean ignoreWarning;
}
