package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;

import jakarta.validation.Valid;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "About To Submit Callback Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CallbackRequest {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("case_details_before")
    private CaseDetails caseDetailsBefore;

    @Valid
    @JsonProperty("case_details")
    private CaseDetails caseDetails;
}
