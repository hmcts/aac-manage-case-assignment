package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.managecase.domain.RequestedCaseUnassignment;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Case Unassignment Request")
public class CaseUnassignmentRequest {

    @JsonProperty("unassignments")
    @NotEmpty(message = "Requested Unassignments can not be empty")
    @ApiModelProperty(value = "Requested Unassignments", required = true)
    private List<RequestedCaseUnassignment> unassignments;

}
