package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.managecase.client.datastore.CallbackCaseDetails;

import javax.validation.Valid;

@Getter
@AllArgsConstructor
@ApiModel("Notice of Change Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoticeOfChangeRequest {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("case_details_before")
    private CallbackCaseDetails caseDetailsBefore;

    @Valid
    @JsonProperty("case_details")
    private CallbackCaseDetails caseDetails;
}
