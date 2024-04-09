package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignedUsers;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "Get case assignments response")
public class GetCaseAssignmentsResponse {

    @JsonProperty("status_message")
    @Schema(description = "Domain Status Message", required = true,
            example = "Case-User-Role assignments returned successfully")
    private String status;

    @JsonProperty("case_assignments")
    @Schema(description = "Case Assignments", required = true)
    private List<CaseAssignedUsers> caseAssignedUsers;
}
