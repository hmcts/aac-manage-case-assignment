package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController;

@Getter
@AllArgsConstructor
@Schema(description = "Case Unassignment Response")
public class CaseUnassignmentResponse {

    @JsonProperty("status_message")
    @Schema(description = "Domain Status Message", 
            requiredMode = RequiredMode.REQUIRED,
            example = CaseAssignmentController.UNASSIGN_ACCESS_MESSAGE)
    private String status;

}
