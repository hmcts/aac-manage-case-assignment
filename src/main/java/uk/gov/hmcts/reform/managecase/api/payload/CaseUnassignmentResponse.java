package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController;

@Getter
@AllArgsConstructor
@ApiModel("Case Unassignment Response")
public class CaseUnassignmentResponse {

    @JsonProperty("status_message")
    @ApiModelProperty(value = "Domain Status Message", required = true,
            example = CaseAssignmentController.UNASSIGN_ACCESS_MESSAGE)
    private String status;

}
