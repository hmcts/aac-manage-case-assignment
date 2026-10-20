package uk.gov.hmcts.reform.managecase.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SubmittedChallengeAnswer {

    @JsonProperty("question_id")
    @Schema(description = "Question ID for which the answer relates to", 
            requiredMode = RequiredMode.REQUIRED, example = "Question1")
    private String questionId;

    @JsonProperty("value")
    @Schema(description = "Submitted answer value", 
            requiredMode = RequiredMode.REQUIRED, example = "Answer for Question1")
    private String value;
}
