package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Case Assignment Response")
public class CaseAssignmentResponse {

    @JsonProperty("status_message")
    @Schema(description = "Domain Status Message", required = true,
            example = "Role [Defendant] from the organisation policy successfully assigned to the assignee.")
    private String status;

}
