package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.managecase.api.controller.CaseAssignedUserRolesController;

@Data
@NoArgsConstructor
@ApiModel("Add Case-Assigned User Roles Response")
public class CaseAssignedUserRolesResponse {

    public CaseAssignedUserRolesResponse(String status) {
        this.status = status;
    }

    @JsonProperty("status_message")
    @ApiModelProperty(value = "Domain Status Message", required = true,
        example = CaseAssignedUserRolesController.ADD_SUCCESS_MESSAGE)
    private String status;

}
