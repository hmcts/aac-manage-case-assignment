package uk.gov.hmcts.reform.managecase.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.managecase.client.definitionstore.DefinitionStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SpringRunner.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DefinitionStoreRepositoryIT {

    @Configuration
    @EnableCaching
    static class CacheConfig {

        @Mock
        private DefinitionStoreApiClient definitionStoreApiClient;

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("challengeQuestions", "caseRoles");
        }

        @Bean
        DefinitionStoreRepository definitionStoreRepository() {
            initMocks(this);
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

    private static final String ANSWER_FIELD_APPLICANT = "${applicant.individual.fullname}|${applicant.company.name}|"
        + "${applicant.soletrader.name}|${OrganisationPolicy1"
        + ".Organisation.OrganisationID}:Applicant";
    private static final String CASE_ID = "12345678";
    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String ZERO = "0";
    private static final String JURISDICTION = ZERO;
    private static final String USER_ID = ZERO;

    @Test
    public void verifyChallengeQuestionsCache() {
        repository.challengeQuestions(CASE_TYPE_ID, CASE_ID);
        assertNotNull(cacheManager.getCache("challengeQuestions").getNativeCache());

        cacheManager.getCache("challengeQuestions").clear();
        assertEquals(cacheManager.getCache("challengeQuestions").getNativeCache().toString(), "{}");

        repository.challengeQuestions(CASE_TYPE_ID, CASE_ID);
        assertNotNull(cacheManager.getCache("challengeQuestions").getNativeCache());
    }

    @Test
    public void verifyCaseRolesCache() {
        repository.caseRoles(USER_ID, JURISDICTION, CASE_TYPE_ID);
        assertNotNull(cacheManager.getCache("caseRoles").getNativeCache());

        cacheManager.getCache("caseRoles").clear();
        assertEquals(cacheManager.getCache("caseRoles").getNativeCache().toString(), "{}");

        repository.caseRoles(USER_ID, JURISDICTION, CASE_TYPE_ID);
        assertNotNull(cacheManager.getCache("caseRoles").getNativeCache());
    }
}
