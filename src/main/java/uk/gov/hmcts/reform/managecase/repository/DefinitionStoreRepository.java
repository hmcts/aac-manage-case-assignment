package uk.gov.hmcts.reform.managecase.repository;

import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;

public interface DefinitionStoreRepository {

    ChallengeQuestionsResult challengeQuestions(String caseTypeId, String challengeQuestionId);

}
