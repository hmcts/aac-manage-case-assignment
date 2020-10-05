package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;


@Data
@AllArgsConstructor
public class ChallengeQuestionsResult {

    @JsonProperty("questions")
    private List<ChallengeQuestion> questions;

}
