package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hibernate.validator.constraints.LuhnCheck;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError;
import uk.gov.hmcts.reform.managecase.domain.SubmittedChallengeAnswer;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import java.util.List;

import static com.fasterxml.jackson.annotation.Nulls.AS_EMPTY;

@Getter
@AllArgsConstructor
@ApiModel("Request Notice Of Change Request")
public class RequestNoticeOfChangeRequest {
    @JsonProperty("case_id")
    @NotEmpty(message = NoCValidationError.NOC_CASE_ID_EMPTY)
    @Size(min = 16, max = 16, message = NoCValidationError.NOC_CASE_ID_INVALID_LENGTH)
    @LuhnCheck(message = NoCValidationError.NOC_CASE_ID_INVALID, ignoreNonDigitCharacters = false)
    @ApiModelProperty(value = "Case ID to Assign Access To", required = true, example = "1583841721773828")
    private String caseId;

    @JsonSetter(nulls = AS_EMPTY)
    @NotEmpty(message = NoCValidationError.NOC_CHALLENGE_QUESTION_ANSWERS_EMPTY)
    @ApiModelProperty(value = "Submitted challenge question answers", required = true)
    private List<@Valid SubmittedChallengeAnswer> answers;
}
