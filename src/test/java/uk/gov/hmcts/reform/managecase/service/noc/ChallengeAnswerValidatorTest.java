package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.reform.managecase.domain.SubmittedChallengeAnswer;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"PMD.JUnitAssertionsShouldIncludeMessage", "PMD.DataflowAnomalyAnalysis", "PMD.TooManyMethods"})
class ChallengeAnswerValidatorTest {

    public static final String QUESTION_ID_1 = "QuestionId1";
    public static final String QUESTION_ID_2 = "QuestionId2";
    public static final String QUESTION_ID_3 = "QuestionId3";
    public static final String TEXT = "Text";

    @InjectMocks
    private ChallengeAnswerValidator challengeAnswerValidator;

    private List<SubmittedChallengeAnswer> answers;
    private SearchResultViewItem caseSearchResult;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);

        answers = new ArrayList<>();
        answers.add(new SubmittedChallengeAnswer(QUESTION_ID_1, " T-e xt'VALUE"));
        answers.add(new SubmittedChallengeAnswer(QUESTION_ID_2, "67890"));
        answers.add(new SubmittedChallengeAnswer(QUESTION_ID_3, "1985-07-25"));

        caseSearchResult = createCase();
    }

    @Test
    void shouldSuccessfullyIdentifyMatchingCaseRole() {
        List<ChallengeQuestion> challengeQuestions = new ArrayList<>();
        challengeQuestions.add(
            challengeQuestion(QUESTION_ID_1, "${TextField}|${SomeOtherField}:[Claimant],${AnotherField}:[Defendant]",
                fieldType(TEXT))
        );
        challengeQuestions.add(
            challengeQuestion(QUESTION_ID_2, "${ComplexField.ComplexNestedField.NestedNumberField}:[Claimant],"
                + "${AnotherField}:[Defendant]", fieldType("Number"))
        );
        challengeQuestions.add(
            challengeQuestion(QUESTION_ID_3, "${AnotherField}:[Defendant],${NonExistingField}|${DateField}:[Claimant]",
                fieldType("Date"))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        String result = challengeAnswerValidator
            .getMatchingCaseRole(challengeQuestionsResult, answers, caseSearchResult);

        assertEquals("[Claimant]", result);
    }

    @Test
    void shouldErrorWhenThereAreMoreAnswersThanQuestions() {
        List<ChallengeQuestion> challengeQuestions = new ArrayList<>();
        challengeQuestions.add(
            challengeQuestion(QUESTION_ID_1, "${Field1}:[Claimant],${Field1}:[Defendant]", fieldType(TEXT))
        );
        challengeQuestions.add(
            challengeQuestion(QUESTION_ID_2, "${Field2}:[Claimant],${Field2}:[Defendant]", fieldType(TEXT))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> challengeAnswerValidator.getMatchingCaseRole(challengeQuestionsResult, answers, caseSearchResult));

        assertEquals(
            "The number of provided answers must match the number of questions - expected 2 answers, received 3",
            exception.getMessage());
    }

    @Test
    void shouldErrorWhenAQuestionHasNotBeenAnswered() {
        List<ChallengeQuestion> challengeQuestions = new ArrayList<>();
        challengeQuestions.add(
            challengeQuestion("OtherQuestionId1", "${Field1}:[Claimant]", fieldType(TEXT))
        );
        challengeQuestions.add(
            challengeQuestion("OtherQuestionId2", "${Field2}:[Claimant]", fieldType(TEXT))
        );
        challengeQuestions.add(
            challengeQuestion("OtherQuestionId3", "${Field3}:[Claimant]", fieldType(TEXT))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> challengeAnswerValidator.getMatchingCaseRole(challengeQuestionsResult, answers, caseSearchResult));

        assertEquals("No answer has been provided for question ID 'OtherQuestionId1'", exception.getMessage());
    }

    @Test
    void shouldErrorWhenAnswersCannotUniquelyIdentifyRole() {
        List<ChallengeQuestion> challengeQuestions = new ArrayList<>();
        challengeQuestions.add(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${TextField}:[Defendant]", fieldType(TEXT))
        );
        challengeQuestions.add(
            challengeQuestion(QUESTION_ID_2, "${ComplexField.ComplexNestedField.NestedNumberField}:[Claimant],"
                + "${ComplexField.ComplexNestedField.NestedNumberField}:[Defendant]", fieldType("Number"))
        );
        challengeQuestions.add(
            challengeQuestion(QUESTION_ID_3, "${DateField}:[Claimant],${DateField}:[Defendant]", fieldType("Date"))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> challengeAnswerValidator.getMatchingCaseRole(challengeQuestionsResult, answers, caseSearchResult));

        assertEquals("The answers did not uniquely identify a litigant", exception.getMessage());
    }

    @Test
    void shouldErrorWhenAnswersDoNotMatchAnyRole() {
        List<ChallengeQuestion> challengeQuestions = new ArrayList<>();
        challengeQuestions.add(
            challengeQuestion(QUESTION_ID_1, "${PhoneUKField}:[Claimant]", fieldType(TEXT))
        );
        challengeQuestions.add(
            challengeQuestion(QUESTION_ID_2, "${AddressUKField.County}:[Claimant]", fieldType(TEXT))
        );
        challengeQuestions.add(
            challengeQuestion(QUESTION_ID_3, "${YesOrNoField}:[Claimant]", fieldType("YesOrNo"))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> challengeAnswerValidator.getMatchingCaseRole(challengeQuestionsResult, answers, caseSearchResult));

        assertEquals("The answers did not match those for any litigant", exception.getMessage());
    }

    @Test
    void shouldSuccessfullyIdentifyRoleWhenFieldValueAndAnswerAreNull() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, null));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextAreaField}:[Claimant]", fieldType(TEXT))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        String result = challengeAnswerValidator
            .getMatchingCaseRole(challengeQuestionsResult, answers, caseSearchResult);

        assertEquals("[Claimant]", result);
    }

    @Test
    void shouldSuccessfullyIdentifyRoleWhenFieldIsNotPersistedAndAnswerIsNull() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, null));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${NonExistingField}:[Claimant]", fieldType(TEXT))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        String result = challengeAnswerValidator
            .getMatchingCaseRole(challengeQuestionsResult, answers, caseSearchResult);

        assertEquals("[Claimant]", result);
    }

    @Test
    void shouldErrorWhenFieldIsNullButAnswerProvided() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, "Answer"));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextAreaField}:[Claimant]", fieldType(TEXT))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> challengeAnswerValidator.getMatchingCaseRole(challengeQuestionsResult, answers, caseSearchResult));

        assertEquals("The answers did not match those for any litigant", exception.getMessage());
    }

    private ChallengeQuestion challengeQuestion(String questionId, String answerField, FieldType answerFieldType) {
        ChallengeQuestion challengeQuestion = new ChallengeQuestion();
        challengeQuestion.setQuestionId(questionId);
        challengeQuestion.setAnswerField(answerField);
        challengeQuestion.setAnswerFieldType(answerFieldType);
        return challengeQuestion;
    }

    private FieldType fieldType(String type) {
        FieldType fieldType = new FieldType();
        fieldType.setType(type);
        return fieldType;
    }

    private SearchResultViewItem createCase() throws JsonProcessingException {
        Map<String, JsonNode> fields = objectMapper.readValue(caseDataString(),
            new TypeReference<Map<String, JsonNode>>() {});
        return new SearchResultViewItem("1", fields, fields);
    }

    private String caseDataString() {
        return "{\n"
            + "    \"DateField\": \"1985-07-25\",\n"
            + "    \"TextField\": \"TextValue\",\n"
            + "    \"EmailField\": \"test@email.com\",\n"
            + "    \"NumberField\": \"12345\",\n"
            + "    \"ComplexField\": {\n"
            + "        \"ComplexNestedField\": {\n"
            + "            \"NestedNumberField\": \"67890\",\n"
            + "            \"NestedCollectionTextField\": []\n"
            + "        },\n"
            + "        \"ComplexTextField\": \"ComplexTextValue\"\n"
            + "    },\n"
            + "    \"PhoneUKField\": \"01234 567890\",\n"
            + "    \"YesOrNoField\": \"No\",\n"
            + "    \"DateTimeField\": \"2020-12-15T12:30:15.000\",\n"
            + "    \"MoneyGBPField\": \"25000\",\n"
            + "    \"TextAreaField\": null,\n"
            + "    \"AddressUKField\": {\n"
            + "        \"County\": \"CountValue\",\n"
            + "        \"Country\": \"CountryValue\",\n"
            + "        \"PostCode\": \"PST CDE\",\n"
            + "        \"PostTown\": \"TownValue\",\n"
            + "        \"AddressLine1\": \"BuildingValue\",\n"
            + "        \"AddressLine2\": \"AddressLine2Value\",\n"
            + "        \"AddressLine3\": \"AddressLine3Value\"\n"
            + "    }\n"
            + "}";
    }
}
