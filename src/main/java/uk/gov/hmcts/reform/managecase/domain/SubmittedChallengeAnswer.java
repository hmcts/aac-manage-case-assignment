package uk.gov.hmcts.reform.managecase.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubmittedChallengeAnswer {

    @JsonProperty("question_id")
    @ApiModelProperty(value = "Question ID for which the answer relates to", required = true, example = "Question1")
    private String questionId;

    @ApiModelProperty(value = "Submitted answer value", required = true, example = "Answer for Question1")
    private String value;
}
