package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.managecase.api.controller.CaseAssignedUserRolesController;

@Data
@NoArgsConstructor
@Schema(description = "Add Case-Assigned User Roles Response")
public class CaseAssignedUserRolesResponse {

    public CaseAssignedUserRolesResponse(String status) {
        this.status = status;
    }

    @JsonProperty("status_message")
    @Schema(description = "Domain Status Message", requiredMode = RequiredMode.REQUIRED,
        example = CaseAssignedUserRolesController.ADD_SUCCESS_MESSAGE)
    private String status;

}
