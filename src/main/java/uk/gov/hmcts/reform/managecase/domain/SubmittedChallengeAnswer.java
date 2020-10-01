package uk.gov.hmcts.reform.managecase.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubmittedChallengeAnswer {

    @JsonProperty("question_id")
    private String questionId;
    private String value;
}
