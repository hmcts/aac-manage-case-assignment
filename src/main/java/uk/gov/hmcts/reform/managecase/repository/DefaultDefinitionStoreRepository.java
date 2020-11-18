package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.definitionstore.DefinitionStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;

import java.util.List;

@Repository
public class DefaultDefinitionStoreRepository implements DefinitionStoreRepository {


    private final DefinitionStoreApiClient definitionStoreApiClient;

    @Autowired
    public DefaultDefinitionStoreRepository(DefinitionStoreApiClient definitionStoreApiClient) {
        this.definitionStoreApiClient = definitionStoreApiClient;
    }

    @Override
    public ChallengeQuestionsResult challengeQuestions(String caseTypeId, String challengeQuestionId) {
        return definitionStoreApiClient.challengeQuestions(caseTypeId, challengeQuestionId);
    }

    @Override
    public List<CaseRole> caseRoles(String userId, String jurisdiction, String caseTypeId) {
        return definitionStoreApiClient.caseRoles(userId, jurisdiction, caseTypeId);
    }
}
