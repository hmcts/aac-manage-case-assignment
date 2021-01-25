package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.definitionstore.DefinitionStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.collect.Maps.newConcurrentMap;

@Repository
public class DefaultDefinitionStoreRepository implements DefinitionStoreRepository {

    public final Map<String, Integer> versions = newConcurrentMap();
    private final DefinitionStoreApiClient definitionStoreApiClient;

    @Autowired
    public DefaultDefinitionStoreRepository(DefinitionStoreApiClient definitionStoreApiClient) {
        this.definitionStoreApiClient = definitionStoreApiClient;
    }

    @Override
    @Cacheable(value = "challengeQuestions",
        condition = "#root.target.hasLatestVersion(#caseTypeId)")
    public ChallengeQuestionsResult challengeQuestions(String caseTypeId, String challengeQuestionId) {
        return definitionStoreApiClient.challengeQuestions(caseTypeId, challengeQuestionId);
    }

    @Override
    @Cacheable(value = "caseRoles", key = "#caseTypeId",
        condition = "#root.target.hasLatestVersion(#caseTypeId)")
    public List<CaseRole> caseRoles(String userId, String jurisdiction, String caseTypeId) {
        return definitionStoreApiClient.caseRoles(userId, jurisdiction, caseTypeId);
    }

    @Override
    public Boolean hasLatestVersion(String caseTypeId) {
        CaseTypeDefinitionVersion caseTypeDefinitionVersion = definitionStoreApiClient.getLatestVersion(caseTypeId);
        if (Objects.equals(versions.get(caseTypeId), caseTypeDefinitionVersion.getVersion())) {
            return true;
        } else {
            versions.put(caseTypeId, caseTypeDefinitionVersion.getVersion());
            return false;
        }
    }
}
