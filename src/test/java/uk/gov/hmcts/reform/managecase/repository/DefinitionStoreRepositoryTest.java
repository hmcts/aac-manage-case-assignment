package uk.gov.hmcts.reform.managecase.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.managecase.client.definitionstore.DefinitionStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

class DefinitionStoreRepositoryTest {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String CASE_ID = "12345678";
    private static final String ANSWER_FIELD = "${applicant.individual.fullname}|${applicant.company.name}|"
        + "${applicant.soletrader.name}:Applicant,${respondent.individual.fullname}|${respondent.company.name}"
        + "|${respondent.soletrader.name}:Respondent";

    @Mock
    private DefinitionStoreApiClient definitionStoreApiClient;

    @InjectMocks
    private DefaultDefinitionStoreRepository repository;


    @BeforeEach
    void setUp() {
        initMocks(this);
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
        ChallengeQuestion challengeQuestion = new ChallengeQuestion(CASE_TYPE_ID, 1,
                                                                    "QuestionText1",
                                                                    fieldType,
                                                                    null,
                                                                    "NoC",
                                                                    ANSWER_FIELD,
                                                                    "QuestionId1",
                                                                    null);
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(
            Arrays.asList(challengeQuestion));
        given(definitionStoreApiClient.challengeQuestions(anyString(), anyString())).willReturn(challengeQuestionsResult);

        // ACT
        ChallengeQuestionsResult result = repository.challengeQuestions(CASE_TYPE_ID, CASE_ID);

        // ASSERT
        assertThat(result).isEqualTo(challengeQuestionsResult);

        verify(definitionStoreApiClient).challengeQuestions(eq(CASE_TYPE_ID), eq(CASE_ID));
    }

}
