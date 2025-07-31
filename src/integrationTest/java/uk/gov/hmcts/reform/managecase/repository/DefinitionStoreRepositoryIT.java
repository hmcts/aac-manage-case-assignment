package uk.gov.hmcts.reform.managecase.repository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.managecase.client.definitionstore.DefinitionStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@ContextConfiguration
public class DefinitionStoreRepositoryIT {

    private static final String CHALLENGE_QUESTIONS = "challengeQuestions";
    private static final String CASE_ROLES = "caseRoles";

    @TestConfiguration
    @EnableCaching
    static class CacheConfig {

        @Mock
        private DefinitionStoreApiClient definitionStoreApiClient;

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CHALLENGE_QUESTIONS, CASE_ROLES);
        }

        @Bean
        DefinitionStoreRepository definitionStoreRepository() {
            openMocks(this);
            FieldType fieldType = FieldType.builder()
                .regularExpression("regular expression")
                .max(null)
                .min(null)
                .id("Number")
                .type("Number")
                .build();
            ChallengeQuestion challengeQuestion = ChallengeQuestion.builder()
                .caseTypeId(CASE_TYPE_ID)
                .challengeQuestionId("NoC")
                .questionText("questionText")
                .answerFieldType(fieldType)
                .answerField(ANSWER_FIELD_APPLICANT)
                .questionId("QuestionId1")
                .order(1).build();
            ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(
                Arrays.asList(challengeQuestion));
            when(definitionStoreApiClient.challengeQuestions(CASE_TYPE_ID, CASE_ID))
                .thenReturn(challengeQuestionsResult);

            List<CaseRole> caseRoleList = Arrays.asList(
                CaseRole.builder().id("APPLICANT").name("Applicant").build());

            when(definitionStoreApiClient.caseRoles(USER_ID, JURISDICTION, CASE_TYPE_ID)).thenReturn(caseRoleList);

            CaseTypeDefinitionVersion caseTypeDefinitionVersion = new CaseTypeDefinitionVersion();
            caseTypeDefinitionVersion.setVersion(123);
            when(definitionStoreApiClient.getLatestVersion(CASE_TYPE_ID)).thenReturn(caseTypeDefinitionVersion);

            return new DefaultDefinitionStoreRepository(definitionStoreApiClient);
        }
    }

    @Autowired
    private DefinitionStoreRepository repository;
    @Autowired
    private CacheManager cacheManager;

    private DefinitionStoreApiClient definitionStoreApiClient;

    private static final String ANSWER_FIELD_APPLICANT = "${applicant.individual.fullname}|${applicant.company.name}|"
        + "${applicant.soletrader.name}|${OrganisationPolicy1"
        + ".Organisation.OrganisationID}:Applicant";
    private static final String CASE_ID = "12345678";
    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String ZERO = "0";
    private static final String JURISDICTION = ZERO;
    private static final String USER_ID = ZERO;

    @Before
    public void setUp() {
        definitionStoreApiClient =
            (DefinitionStoreApiClient) ReflectionTestUtils.getField(repository, "definitionStoreApiClient");
    }

    @Test
    public void verifyChallengeQuestionsCacheContents() {
        repository.challengeQuestions(CASE_TYPE_ID, CASE_ID);
        assertNotNull(cacheManager.getCache(CHALLENGE_QUESTIONS).getNativeCache());

        cacheManager.getCache(CHALLENGE_QUESTIONS).clear();
        assertEquals(cacheManager.getCache(CHALLENGE_QUESTIONS).getNativeCache().toString(), "{}");
    }

    @Test
    public void verifyChallengeQuestionsCache() {
        repository.challengeQuestions(CASE_TYPE_ID, CASE_ID);
        repository.challengeQuestions(CASE_TYPE_ID, CASE_ID);
        repository.challengeQuestions(CASE_TYPE_ID, CASE_ID);
        repository.challengeQuestions(CASE_TYPE_ID, CASE_ID);
        verify(definitionStoreApiClient, times(1)).challengeQuestions(CASE_TYPE_ID, CASE_ID);
    }

    @Test
    public void verifyCaseRolesCacheContents() {
        repository.caseRoles(USER_ID, JURISDICTION, CASE_TYPE_ID);
        assertNotNull(cacheManager.getCache(CASE_ROLES).getNativeCache());

        cacheManager.getCache(CASE_ROLES).clear();
        assertEquals(cacheManager.getCache(CASE_ROLES).getNativeCache().toString(), "{}");

        repository.caseRoles(USER_ID, JURISDICTION, CASE_TYPE_ID);
        assertNotNull(cacheManager.getCache(CASE_ROLES).getNativeCache());
    }

    @Test
    public void verifyCaseRolesCache() {
        repository.caseRoles(USER_ID, JURISDICTION, CASE_TYPE_ID);
        repository.caseRoles(USER_ID, JURISDICTION, CASE_TYPE_ID);
        repository.caseRoles(USER_ID, JURISDICTION, CASE_TYPE_ID);
        repository.caseRoles(USER_ID, JURISDICTION, CASE_TYPE_ID);
        verify(definitionStoreApiClient, times(1)).caseRoles(USER_ID, JURISDICTION, CASE_TYPE_ID);
    }
}
