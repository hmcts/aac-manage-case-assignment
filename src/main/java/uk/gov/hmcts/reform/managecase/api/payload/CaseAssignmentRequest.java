package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hibernate.validator.constraints.LuhnCheck;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;

@Getter
@AllArgsConstructor
@ApiModel("Case Assignment Request")
public class CaseAssignmentRequest {

    @JsonProperty("case_type_id")
    @NotEmpty(message = "Case type ID can not be empty")
    @ApiModelProperty(value = "Case type ID of the requested case", required = true, example = "PROBATE-TEST")
    private String caseTypeId;

    @JsonProperty("case_id")
    @NotEmpty(message = ValidationError.CASE_ID_EMPTY)
    @Size(min = 16, max = 16, message = ValidationError.CASE_ID_INVALID_LENGTH)
    @LuhnCheck(message = ValidationError.CASE_ID_INVALID)
    @Pattern(regexp = "\\d+", message = "Case ID should contain digits only")
    @ApiModelProperty(value = "Case ID to Assign Access To", required = true, example = "1583841721773828")
    private String caseId;

    @JsonProperty("assignee_id")
    @NotEmpty(message = "IDAM Assignee ID can not be empty")
    @ApiModelProperty(value = "IDAM ID of the Assign User", required = true, example = "ecb5edf4-2f5f-4031-a0ec")
    private String assigneeId;
}
