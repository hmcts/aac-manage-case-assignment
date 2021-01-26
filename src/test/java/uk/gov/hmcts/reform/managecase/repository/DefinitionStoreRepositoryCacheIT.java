package uk.gov.hmcts.reform.managecase.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.hmcts.reform.managecase.client.definitionstore.DefinitionStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseTypeDefinitionVersion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class DefinitionStoreRepositoryCacheIT {

    @Configuration
    @EnableCaching
    static class CacheConfig {

        @Bean
        DefinitionStoreApiClient definitionStoreApiClient() {
            return mock(DefinitionStoreApiClient.class);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("challengeQuestions");
        }

        @Bean
        DefinitionStoreRepository definitionStoreRepository(DefinitionStoreApiClient definitionStoreApiClient) {
            return new DefaultDefinitionStoreRepository(definitionStoreApiClient);
        }
    }

    private static final String CHALLENGE_ID = "A12345678";
    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";

    @Autowired
    private DefinitionStoreApiClient client;
    @Autowired
    private DefinitionStoreRepository repository;

    private CaseTypeDefinitionVersion caseTypeDefinitionVersion;

    @Before
    public void setUp() {
        caseTypeDefinitionVersion = new CaseTypeDefinitionVersion(123);
    }

    @Test
    public void verifyChallengeQuestionsCache() {
        // first call
        when(client.getLatestVersion(CASE_TYPE_ID)).thenReturn(caseTypeDefinitionVersion);

        repository.challengeQuestions(CASE_TYPE_ID, CHALLENGE_ID);
        // FIXME: looks like SpEl calling twice instead of one when cache miss
        verify(client, times(2)).getLatestVersion(CASE_TYPE_ID);
        verify(client, times(1)).challengeQuestions(CASE_TYPE_ID, CHALLENGE_ID);

        // second call with same params
        Mockito.reset(client);
        when(client.getLatestVersion(CASE_TYPE_ID)).thenReturn(caseTypeDefinitionVersion);

        repository.challengeQuestions(CASE_TYPE_ID, CHALLENGE_ID);
        verify(client, times(1)).getLatestVersion(CASE_TYPE_ID);
        verify(client, never()).challengeQuestions(CASE_TYPE_ID, CHALLENGE_ID);

        // third call with different params
        Mockito.reset(client);
        when(client.getLatestVersion(CASE_TYPE_ID)).thenReturn(caseTypeDefinitionVersion);

        repository.challengeQuestions(CASE_TYPE_ID, "differentChallengeId");
        // FIXME: looks like SpEl calling twice instead of one when cache miss
        verify(client, times(2)).getLatestVersion(CASE_TYPE_ID);
        verify(client, times(1)).challengeQuestions(CASE_TYPE_ID,
            "differentChallengeId");
    }
}
