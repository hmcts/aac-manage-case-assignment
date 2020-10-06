package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

import static com.fasterxml.jackson.annotation.Nulls.AS_EMPTY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.EMPTY_REQUESTED_UNASSIGNMENTS_LIST;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Case Unassignment Request")
public class CaseUnassignmentRequest {

    @JsonProperty("unassignments")
    @JsonSetter(nulls = AS_EMPTY)
    @NotEmpty(message = EMPTY_REQUESTED_UNASSIGNMENTS_LIST)
    @ApiModelProperty(value = "Requested Unassignments", required = true)
    private List<@Valid RequestedCaseUnassignment> unassignments;

}
