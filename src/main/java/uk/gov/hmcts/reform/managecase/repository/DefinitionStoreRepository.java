package uk.gov.hmcts.reform.managecase.repository;

import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;

import java.util.List;

public interface DefinitionStoreRepository {

    ChallengeQuestionsResult challengeQuestions(String caseTypeId, String challengeQuestionId);

    List<CaseRole> caseRoles(String userId, String jurisdiction, String caseTypeId);

}
