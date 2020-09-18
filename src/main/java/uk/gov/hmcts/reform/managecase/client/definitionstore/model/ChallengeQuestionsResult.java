package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class ChallengeQuestionsResult {

    private List<ChallengeQuestion> questions;
}
