package uk.gov.hmcts.reform.managecase.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@ApiModel("Requested Unassignment From Case")
public class RequestedCaseUnassignment {

    @JsonProperty("case_id")
    @ApiModelProperty(value = "Case ID to Unassign Access To", required = true, example = "1583841721773828")
    private String caseId;

    @JsonProperty("assignee_id")
    @ApiModelProperty(value = "IDAM ID of the User to Unassign", required = true, example = "ecb5edf4-2f5f-4031-a0ec")
    private String assigneeId;

    @JsonProperty("case_roles")
    @ApiModelProperty(
        value = "Case Roles to Unassign",
        allowEmptyValue = true,
        example = "[\"[Claimant]\",\"[Defendant]\"]"
    )
    private List<String> caseRoles;

    @JsonIgnore
    public boolean hasCaseRoles() {
        return this.caseRoles != null && !this.caseRoles.isEmpty();
    }
}
