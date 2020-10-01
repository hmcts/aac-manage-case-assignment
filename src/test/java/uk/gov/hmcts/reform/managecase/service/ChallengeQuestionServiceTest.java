package uk.gov.hmcts.reform.managecase.service;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChallengeQuestionServiceTest {

    @InjectMocks
    private ChallengeQuestionService challengeQuestionService;

    private List<SubmittedChallengeAnswer> answers;
    private SearchResultViewItem caseSearchResult;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);

        answers = new ArrayList<>();
        answers.add(new SubmittedChallengeAnswer("QuestionId1", " T-e xt'VALUE"));
        answers.add(new SubmittedChallengeAnswer("QuestionId2", "67890"));
        answers.add(new SubmittedChallengeAnswer("QuestionId3", "1985-07-25"));

        caseSearchResult = createCase();
    }

    @Test
    void compareAnswersSuccess() {
        List<ChallengeQuestion> challengeQuestions = new ArrayList<>();
        challengeQuestions.add(
            challengeQuestion("QuestionId1", "${TextField}|${SomeOtherField}:[Claimant],${AnotherField}:[Defendant]",
                fieldType("Text"))
        );
        challengeQuestions.add(
            challengeQuestion("QuestionId2", "${ComplexField.ComplexNestedField.NestedNumberField}:[Claimant],"
                + "${AnotherField}:[Defendant]", fieldType("Number"))
        );
        challengeQuestions.add(
            challengeQuestion("QuestionId3", "${AnotherField}:[Defendant],${NonExistingField}|${DateField}:[Claimant]",
                fieldType("Date"))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        String result = challengeQuestionService
            .getMatchingCaseRole(challengeQuestionsResult, answers, caseSearchResult);

        assertEquals("[Claimant]", result);
    }

    @Test
    void compareAnswersMoreAnswersThanQuestions() {
        List<ChallengeQuestion> challengeQuestions = new ArrayList<>();
        challengeQuestions.add(
            challengeQuestion("QuestionId1", "${Field1}:[Claimant],${Field1}:[Defendant]", fieldType("Text"))
        );
        challengeQuestions.add(
            challengeQuestion("QuestionId2", "${Field2}:[Claimant],${Field2}:[Defendant]", fieldType("Text"))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> challengeQuestionService.getMatchingCaseRole(challengeQuestionsResult, answers, caseSearchResult));

        assertEquals(
            "The number of provided answers must match the number of questions - expected 2 answers, received 3",
            exception.getMessage());
    }

    @Test
    void compareAnswersQuestionNotFound() {
        List<ChallengeQuestion> challengeQuestions = new ArrayList<>();
        challengeQuestions.add(
            challengeQuestion("OtherQuestionId1", "${Field1}:[Claimant]", fieldType("Text"))
        );
        challengeQuestions.add(
            challengeQuestion("OtherQuestionId2", "${Field2}:[Claimant]", fieldType("Text"))
        );
        challengeQuestions.add(
            challengeQuestion("OtherQuestionId3", "${Field3}:[Claimant]", fieldType("Text"))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> challengeQuestionService.getMatchingCaseRole(challengeQuestionsResult, answers, caseSearchResult));

        assertEquals("No answer has been provided for question ID 'OtherQuestionId1'", exception.getMessage());
    }

    @Test
    void compareAnswersCannotUniquelyIdentifyRole() {
        List<ChallengeQuestion> challengeQuestions = new ArrayList<>();
        challengeQuestions.add(
            challengeQuestion("QuestionId1", "${TextField}:[Claimant],${TextField}:[Defendant]", fieldType("Text"))
        );
        challengeQuestions.add(
            challengeQuestion("QuestionId2", "${ComplexField.ComplexNestedField.NestedNumberField}:[Claimant],"
                + "${ComplexField.ComplexNestedField.NestedNumberField}:[Defendant]", fieldType("Number"))
        );
        challengeQuestions.add(
            challengeQuestion("QuestionId3", "${DateField}:[Claimant],${DateField}:[Defendant]", fieldType("Date"))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> challengeQuestionService.getMatchingCaseRole(challengeQuestionsResult, answers, caseSearchResult));

        assertEquals("The answers did not uniquely identify a litigant", exception.getMessage());
    }

    @Test
    void compareAnswersCannotIdentifyRole() {
        List<ChallengeQuestion> challengeQuestions = new ArrayList<>();
        challengeQuestions.add(
            challengeQuestion("QuestionId1", "${PhoneUKField}:[Claimant]", fieldType("Text"))
        );
        challengeQuestions.add(
            challengeQuestion("QuestionId2", "${AddressUKField.County}:[Claimant]", fieldType("Text"))
        );
        challengeQuestions.add(
            challengeQuestion("QuestionId3", "${YesOrNoField}:[Claimant]", fieldType("YesOrNo"))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> challengeQuestionService.getMatchingCaseRole(challengeQuestionsResult, answers, caseSearchResult));

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
