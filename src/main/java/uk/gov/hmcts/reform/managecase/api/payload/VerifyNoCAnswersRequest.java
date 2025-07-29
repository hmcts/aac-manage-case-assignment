package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.LuhnCheck;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError;
import uk.gov.hmcts.reform.managecase.domain.SubmittedChallengeAnswer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

import static com.fasterxml.jackson.annotation.Nulls.AS_EMPTY;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Verify Notice of Change Answers Request")
public class VerifyNoCAnswersRequest {

    @JsonProperty("case_id")
    @NotEmpty(message = NoCValidationError.NOC_CASE_ID_EMPTY)
    @Size(min = 16, max = 16, message = NoCValidationError.NOC_CASE_ID_INVALID_LENGTH)
    @LuhnCheck(message = NoCValidationError.NOC_CASE_ID_INVALID)
    @Schema(description = "Case ID to verify NoC challengeAnswers for", 
            requiredMode = RequiredMode.REQUIRED,
        example = "1583841721773828")
    private String caseId;

    @JsonSetter(nulls = AS_EMPTY)
    @NotEmpty(message = NoCValidationError.NOC_CHALLENGE_QUESTION_ANSWERS_EMPTY)
    @Schema(description = "Submitted challenge question answers", 
            requiredMode = RequiredMode.REQUIRED)
    private List<@Valid SubmittedChallengeAnswer> answers;
}
