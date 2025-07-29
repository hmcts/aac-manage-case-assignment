package uk.gov.hmcts.reform.managecase.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.managecase.client.definitionstore.DefinitionStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

class DefinitionStoreRepositoryTest {

    private static final String JURISDICTION = "JURISDICTION";
    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String CASE_ID = "12345678";
    private static final String USER_ID = "ecb5edf4-2f5f-4031-a0ec";
    private static final String ANSWER_FIELD = "${applicant.individual.fullname}|${applicant.company.name}|"
        + "${applicant.soletrader.name}:Applicant,${respondent.individual.fullname}|${respondent.company.name}"
        + "|${respondent.soletrader.name}:Respondent";

    @Mock
    private DefinitionStoreApiClient definitionStoreApiClient;

    @InjectMocks
    private DefaultDefinitionStoreRepository repository;


    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    @DisplayName("Get Challenge questions")
    void shouldGetChallengeQuestions() {
        // ARRANGE
        FieldType fieldType = FieldType.builder()
            .max(null)
            .min(null)
            .id("Number")
            .type("Number")
            .build();
        ChallengeQuestion challengeQuestion = ChallengeQuestion.builder()
            .caseTypeId(CASE_TYPE_ID)
            .challengeQuestionId("QuestionId1")
            .questionText("QuestionText1")
            .answerFieldType(fieldType)
            .answerField(ANSWER_FIELD)
            .questionId("NoC").build();
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(
            Arrays.asList(challengeQuestion));
        given(definitionStoreApiClient.challengeQuestions(anyString(), anyString()))
            .willReturn(challengeQuestionsResult);

        // ACT
        ChallengeQuestionsResult result = repository.challengeQuestions(CASE_TYPE_ID, CASE_ID);

        // ASSERT
        assertThat(result).isEqualTo(challengeQuestionsResult);

        verify(definitionStoreApiClient).challengeQuestions(eq(CASE_TYPE_ID), eq(CASE_ID));
    }

    @Test
    @DisplayName("Get Case Roles")
    void shouldGetCaseRoles() {
        List<CaseRole> caseRoles = Arrays.asList(
            CaseRole.builder().build(),
            CaseRole.builder().build(),
            CaseRole.builder().build()
        );
        given(definitionStoreApiClient.caseRoles(anyString(), anyString(), anyString()))
            .willReturn(caseRoles);

        List<CaseRole> result = repository.caseRoles(USER_ID, JURISDICTION, CASE_TYPE_ID);

        assertThat(result).isEqualTo(caseRoles);

        verify(definitionStoreApiClient).caseRoles(eq(USER_ID), eq(JURISDICTION), eq(CASE_TYPE_ID));
    }
}
