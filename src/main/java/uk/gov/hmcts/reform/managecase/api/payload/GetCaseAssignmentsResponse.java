package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignedUsers;

import java.util.List;

@Getter
@AllArgsConstructor
@ApiModel("Get case assignments response")
public class GetCaseAssignmentsResponse {

    @JsonProperty("status_message")
    @ApiModelProperty(value = "Domain Status Message", required = true,
            example = "Case-User-Role assignments returned successfully")
    private String status;

    @JsonProperty("case_assignments")
    @ApiModelProperty(value = "Case Assignments", required = true)
    private List<CaseAssignedUsers> caseAssignedUsers;
}
