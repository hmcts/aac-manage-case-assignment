package uk.gov.hmcts.reform.managecase.api.payload;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;

import javax.validation.constraints.NotNull;

@Getter
@AllArgsConstructor
@ApiModel("Verify Notice of Change Answers Request")
public class ApplyNoCDecisionRequest {

    @NotNull(message = "TODO")
    @ApiModelProperty(value = "Case details", required = true)
    private CaseDetails caseDetails;

    // TODO: Match callback format
}
