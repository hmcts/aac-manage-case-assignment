package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.managecase.domain.Organisation;

import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.VERIFY_NOC_ANSWERS_MESSAGE;

@Getter
@AllArgsConstructor
@Schema(description = "Verify Notice of Change Answers Response")
public class VerifyNoCAnswersResponse {

    @JsonProperty("status_message")
    @Schema(description = "Domain status message", 
            requiredMode = RequiredMode.REQUIRED, example = VERIFY_NOC_ANSWERS_MESSAGE)
    private String status;

    @Schema(description = "Organisation for identified case role", 
            requiredMode = RequiredMode.REQUIRED)
    private Organisation organisation;
}
