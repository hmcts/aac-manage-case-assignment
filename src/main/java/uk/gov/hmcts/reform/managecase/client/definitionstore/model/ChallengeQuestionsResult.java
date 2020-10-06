package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ChallengeQuestionsResult {

    @JsonProperty("questions")
    private List<ChallengeQuestion> questions;
}
