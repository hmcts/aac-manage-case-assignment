package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;

import javax.validation.constraints.NotNull;

import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_DETAILS_REQUIRED;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Verify Notice of Change Answers Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplyNoCDecisionRequest {

    @JsonProperty("case_details")
    @NotNull(message = CASE_DETAILS_REQUIRED)
    @ApiModelProperty(value = "Case details", required = true)
    private CaseDetails caseDetails;
}
