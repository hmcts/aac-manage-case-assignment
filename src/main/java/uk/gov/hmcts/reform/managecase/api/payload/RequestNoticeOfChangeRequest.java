package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hibernate.validator.constraints.LuhnCheck;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Getter
@AllArgsConstructor
@ApiModel("Request Notice Of Change Request")
public class RequestNoticeOfChangeRequest {
    @JsonProperty("case_id")
    @NotEmpty(message = ValidationError.CASE_ID_EMPTY)
    @Size(min = 16, max = 16, message = ValidationError.CASE_ID_INVALID_LENGTH)
    @LuhnCheck(message = ValidationError.CASE_ID_INVALID, ignoreNonDigitCharacters = false)
    @ApiModelProperty(value = "Case ID to Assign Access To", required = true, example = "1583841721773828")
    private String caseId;
}
