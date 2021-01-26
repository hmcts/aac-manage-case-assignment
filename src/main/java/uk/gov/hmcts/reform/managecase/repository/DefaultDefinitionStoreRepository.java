package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(value = "challengeQuestions",
        key = "{#caseTypeId, #root.target.getLatestVersion(#caseTypeId)}")
    public ChallengeQuestionsResult challengeQuestions(String caseTypeId, String challengeQuestionId) {
        return definitionStoreApiClient.challengeQuestions(caseTypeId, challengeQuestionId);
    }

    @Override
    @Cacheable(value = "caseRoles",
        key = "{#caseTypeId, #root.target.getLatestVersion(#caseTypeId)}")
    public List<CaseRole> caseRoles(String userId, String jurisdiction, String caseTypeId) {
        return definitionStoreApiClient.caseRoles(userId, jurisdiction, caseTypeId);
    }

    @Override
    public Integer getLatestVersion(String caseTypeId) {
        return definitionStoreApiClient.getLatestVersion(caseTypeId).getVersion();
    }
}
