package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;

import javax.validation.Valid;

@Getter
@AllArgsConstructor
@ApiModel("Check Notice of Change Approval Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckNoticeOfChangeApprovalRequest {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("case_details_before")
    private CaseDetails caseDetailsBefore;

    @Valid
    @JsonProperty("case_details")
    private CaseDetails caseDetails;
}
