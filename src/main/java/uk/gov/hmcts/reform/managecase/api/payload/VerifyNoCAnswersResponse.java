package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.managecase.domain.Organisation;

import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.VERIFY_NOC_ANSWERS_MESSAGE;

@Getter
@AllArgsConstructor
@ApiModel("Verify Notice of Change Answers Response")
public class VerifyNoCAnswersResponse {

    @JsonProperty("status_message")
    @ApiModelProperty(value = "Domain status message", required = true, example = VERIFY_NOC_ANSWERS_MESSAGE)
    private String status;

    @ApiModelProperty(value = "Organisation for identified case role", required = true)
    private Organisation organisation;
}
