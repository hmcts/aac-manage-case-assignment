package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.definitionstore.DefinitionStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

@Repository
public class DefaultDefinitionStoreRepository implements DefinitionStoreRepository {

    public final Map<String, Integer> versions = newHashMap();
    private final DefinitionStoreApiClient definitionStoreApiClient;

    @Autowired
    public DefaultDefinitionStoreRepository(DefinitionStoreApiClient definitionStoreApiClient) {
        this.definitionStoreApiClient = definitionStoreApiClient;
    }

    @Override
    @Cacheable(value = "challengeQuestions",
        condition = "#root.target.getLatestVersion(#caseTypeId) == #root.target.versions[#caseTypeId]")
    public ChallengeQuestionsResult challengeQuestions(String caseTypeId, String challengeQuestionId) {
        versions.put(caseTypeId, getLatestVersion(caseTypeId));
        return definitionStoreApiClient.challengeQuestions(caseTypeId, challengeQuestionId);
    }

    @Override
    @Cacheable(value = "caseRoles", key = "#caseTypeId",
        condition = "#root.target.getLatestVersion(#caseTypeId) == #root.target.versions[#caseTypeId]")
    public List<CaseRole> caseRoles(String userId, String jurisdiction, String caseTypeId) {
        versions.put(caseTypeId, getLatestVersion(caseTypeId));
        return definitionStoreApiClient.caseRoles(userId, jurisdiction, caseTypeId);
    }

    @Override
    public Integer getLatestVersion(String caseTypeId) {
        return definitionStoreApiClient.getLatestVersion(caseTypeId).getVersion();
    }
}
