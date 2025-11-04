package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCException;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.reform.managecase.domain.SubmittedChallengeAnswer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.ANSWERS_NOT_IDENTIFY_LITIGANT;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.ANSWERS_NOT_MATCH_LITIGANT;

@SuppressWarnings({"PMD.JUnitAssertionsShouldIncludeMessage", "PMD.DataflowAnomalyAnalysis",
    "PMD.TooManyMethods", "PMD.UseConcurrentHashMap"})
class ChallengeAnswerValidatorTest {

    public static final String QUESTION_ID_1 = "QuestionId1";
    public static final String QUESTION_ID_2 = "QuestionId2";
    public static final String QUESTION_ID_3 = "QuestionId3";
    public static final String TEXT = "Text";

    @InjectMocks
    private ChallengeAnswerValidator challengeAnswerValidator;

    private List<SubmittedChallengeAnswer> answers;
    private CaseDetails caseDetails;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws JsonProcessingException {
        MockitoAnnotations.openMocks(this);

        answers = new ArrayList<>();
        answers.add(new SubmittedChallengeAnswer(QUESTION_ID_1, " T-e xt'VALUE"));
        answers.add(new SubmittedChallengeAnswer(QUESTION_ID_2, "67890"));
        answers.add(new SubmittedChallengeAnswer(QUESTION_ID_3, "1985-07-25"));

        caseDetails = createCase();
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
            .getMatchingCaseRole(challengeQuestionsResult, answers, caseDetails);

        assertThat(result, is("[Claimant]"));
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

        NoCException exception = assertThrows(NoCException.class,
            () -> challengeAnswerValidator.getMatchingCaseRole(challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is("answers-mismatch-questions"));

        assertThat(exception.getErrorMessage(),
            is("The number of provided answers must match the number of questions - expected 2 answers, received 3"));

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

        NoCException exception = assertThrows(NoCException.class,
            () -> challengeAnswerValidator.getMatchingCaseRole(challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorMessage(), is("No answer has been provided for question ID 'OtherQuestionId1'"));

        assertThat(exception.getErrorCode(), is("no-answer-provided-for-question"));
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

        NoCException exception = assertThrows(NoCException.class,
            () -> challengeAnswerValidator.getMatchingCaseRole(challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_IDENTIFY_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_IDENTIFY_LITIGANT.getErrorMessage()));
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

        NoCException exception = assertThrows(NoCException.class,
            () -> challengeAnswerValidator.getMatchingCaseRole(challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorMessage()));
    }

    @Test
    void shouldSuccessfullyIdentifyRoleWhenFieldValueAndAnswerAreNull() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, null));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextAreaField}:[Claimant]", fieldType(TEXT))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        String result = challengeAnswerValidator
            .getMatchingCaseRole(challengeQuestionsResult, answers, caseDetails);

        assertThat(result, is("[Claimant]"));
    }

    @Test
    void shouldSuccessfullyIdentifyRoleWhenFieldIsNotPersistedAndAnswerIsNull() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, null));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${NonExistingField}:[Claimant]", fieldType(TEXT))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        String result = challengeAnswerValidator
            .getMatchingCaseRole(challengeQuestionsResult, answers, caseDetails);

        assertThat(result, is("[Claimant]"));
    }

    @Test
    void shouldErrorWhenFieldIsNullButAnswerProvided() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, "Answer"));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextAreaField}:[Claimant]", fieldType(TEXT))
        );
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        NoCException exception = assertThrows(NoCException.class,
            () -> challengeAnswerValidator.getMatchingCaseRole(challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorMessage()));
    }

    @Test
    void shouldErrorWhenCaseFieldIsPresentAndAnswerIsNull_IgnoreNullFieldIsTrue() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, ""));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${AnotherField1}:[Defendant],"
                                  + "${TextField}:[respondent1],${TextField}:[respondent2]", fieldType(TEXT)));
        challengeQuestions.get(0).setIgnoreNullFields(true);
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        NoCException exception = assertThrows(NoCException.class,
                                              () -> challengeAnswerValidator.getMatchingCaseRole(
                                                  challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorMessage()));
    }

    @Test
    void shouldPassWhenCaseFieldIsPresentAndAnswerIsNull_IgnoreNullFieldIsFalse() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, ""));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${AnotherField1}:[Defendant],"
                                  + "${TextField}:[respondent1],${TextField}:[respondent2]", fieldType(TEXT)));
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        String result = challengeAnswerValidator
            .getMatchingCaseRole(challengeQuestionsResult, answers, caseDetails);

        assertThat(result, is("[Defendant]"));
    }

    @Test
    void shouldErrorWhenCaseFieldIsPresentAndAnswerIsNotNull_IgnoreNullFieldIsTrue() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, "Name2"));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${AnotherField1}:[Defendant],"
                                  + "${TextField}:[respondent1],${TextField}:[respondent2]", fieldType(TEXT)));
        challengeQuestions.get(0).setIgnoreNullFields(true);
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        NoCException exception = assertThrows(NoCException.class,
                                              () -> challengeAnswerValidator.getMatchingCaseRole(
                                                  challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorMessage()));
    }

    @Test
    void shouldErrorWhenCaseFieldIsPresentAndAnswerIsNotNull_IgnoreNullFieldIsFalse() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, "Name2"));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${AnotherField1}:[Defendant],"
                                  + "${TextField}:[respondent1],${TextField}:[respondent2]", fieldType(TEXT)));
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        NoCException exception = assertThrows(NoCException.class,
                                              () -> challengeAnswerValidator.getMatchingCaseRole(
                                                  challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorMessage()));
    }

    @Test
    void shouldErrorWhenCaseFieldIsNotPresentAndAnswerIsNull_IgnoreNullFieldIsTrue() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, ""));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${ApplicantField}:[Defendant],"
                                  + "${TextField}:[respondent1],${TextField}:[respondent2]", fieldType(TEXT)));
        challengeQuestions.get(0).setIgnoreNullFields(true);
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        NoCException exception = assertThrows(NoCException.class,
                                              () -> challengeAnswerValidator.getMatchingCaseRole(
                                                  challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorMessage()));
    }

    @Test
    void shouldErrorWhenCaseFieldIsNotPresentAndAnswerIsNotNull_IgnoreNullFieldIsTrue() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, "Name2"));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${ApplicantField}:[Defendant],"
                                  + "${TextField}:[respondent1],${TextField}:[respondent2]", fieldType(TEXT)));
        challengeQuestions.get(0).setIgnoreNullFields(true);
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        NoCException exception = assertThrows(NoCException.class,
                                              () -> challengeAnswerValidator.getMatchingCaseRole(
                                                  challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorMessage()));
    }

    @Test
    void shouldPassWhenCaseFieldIsNotPresentAndAnswerIsNull_IgnoreNullFieldIsFalse() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, ""));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${ApplicantField}:[Defendant],"
                                  + "${TextField}:[respondent1],${TextField}:[respondent2]", fieldType(TEXT)));
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        String result = challengeAnswerValidator
            .getMatchingCaseRole(challengeQuestionsResult, answers, caseDetails);

        assertThat(result, is("[Defendant]"));
    }

    @Test
    void shouldErrorWhenCaseFieldIsNotPresentAndAnswerIsNotNull_IgnoreNullFieldIsFalse() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, "Name2"));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${ApplicantField}:[Defendant],"
                                  + "${TextField}:[respondent1],${TextField}:[respondent2]", fieldType(TEXT)));
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        NoCException exception = assertThrows(NoCException.class,
                                              () -> challengeAnswerValidator.getMatchingCaseRole(
                                                  challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorMessage()));
    }

    @Test
    void shouldErrorWhenAllCaseFieldsPresentAndAnswerIsNull_IgnoreNullFieldIsFalse() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, ""));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${Applicant2Field}:[Defendant],"
                + "${TextField}:[respondent1],${TextField}:[respondent2]", fieldType(TEXT)));
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        NoCException exception = assertThrows(NoCException.class,
                                              () -> challengeAnswerValidator.getMatchingCaseRole(
                                                  challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorMessage()));
    }

    @Test
    void shouldPassWhenCaseFieldsPresentAndAnswerMatches_IgnoreNullFieldIsFalse() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, "Name2"));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${Applicant2Field}:[Defendant],"
                + "${TextField}:[respondent1],${TextField}:[respondent2]", fieldType(TEXT)));
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        String result = challengeAnswerValidator
            .getMatchingCaseRole(challengeQuestionsResult, answers, caseDetails);

        assertThat(result, is("[Defendant]"));
    }

    @Test
    void shouldErrorWhenAllCaseFieldsPresentAndAnswerIsNull_IgnoreNullFieldIsTrue() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, ""));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${Applicant2Field}:[Defendant],"
                + "${TextField}:[respondent1],${TextField}:[respondent2]", fieldType(TEXT)));
        challengeQuestions.get(0).setIgnoreNullFields(true);
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        NoCException exception = assertThrows(NoCException.class,
                                              () -> challengeAnswerValidator.getMatchingCaseRole(
                                                  challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorMessage()));
    }

    @Test
    void shouldPassWhenCaseFieldsPresentAndAnswerMatches_IgnoreNullFieldIsTrue() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, "Name2"));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${Applicant2Field}:[Defendant],"
                + "${TextField}:[respondent1],${TextField}:[respondent2]", fieldType(TEXT)));
        challengeQuestions.get(0).setIgnoreNullFields(true);
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        String result = challengeAnswerValidator
            .getMatchingCaseRole(challengeQuestionsResult, answers, caseDetails);

        assertThat(result, is("[Defendant]"));
    }

    @Test
    void shouldErrorWhenAllCaseFieldsNotPresentAndAnswerIsNull_IgnoreNullFieldIsTrue() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, ""));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${TextField}:[respondent2]",
                              fieldType(TEXT)));
        challengeQuestions.get(0).setIgnoreNullFields(true);
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        NoCException exception = assertThrows(NoCException.class,
                                              () -> challengeAnswerValidator.getMatchingCaseRole(
                                                  challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorMessage()));
    }

    @Test
    void shouldErrorWhenAllCaseFieldsNotPresentAndAnswerIsNotNull_IgnoreNullFieldIsTrue() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, "Name2"));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${TextField}:[respondent2]",
                              fieldType(TEXT)));
        challengeQuestions.get(0).setIgnoreNullFields(true);
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        NoCException exception = assertThrows(NoCException.class,
                                              () -> challengeAnswerValidator.getMatchingCaseRole(
                                                  challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorMessage()));
    }

    @Test
    void shouldErrorWhenAllCaseFieldsNotPresentAndAnswerIsNull_IgnoreNullFieldIsFalse() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, ""));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${TextField}:[respondent2]",
                              fieldType(TEXT)));
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        NoCException exception = assertThrows(NoCException.class,
                                              () -> challengeAnswerValidator.getMatchingCaseRole(
                                                  challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorMessage()));
    }

    @Test
    void shouldErrorWhenAllCaseFieldsNotPresentAndAnswerIsNotNull_IgnoreNullFieldIsFalse() {
        answers = singletonList(new SubmittedChallengeAnswer(QUESTION_ID_1, "Name2"));
        List<ChallengeQuestion> challengeQuestions = singletonList(
            challengeQuestion(QUESTION_ID_1, "${TextField}:[Claimant],${TextField}:[respondent2]",
                              fieldType(TEXT)));
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(challengeQuestions);

        NoCException exception = assertThrows(NoCException.class,
                                              () -> challengeAnswerValidator.getMatchingCaseRole(
                                                  challengeQuestionsResult, answers, caseDetails));

        assertThat(exception.getErrorCode(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorCode()));

        assertThat(exception.getErrorMessage(), is(ANSWERS_NOT_MATCH_LITIGANT.getErrorMessage()));
    }


    private ChallengeQuestion challengeQuestion(String questionId, String answerField, FieldType answerFieldType) {
        ChallengeQuestion challengeQuestion = ChallengeQuestion.builder()
            .questionId(questionId)
            .answerFieldType(answerFieldType)
            .build();
        challengeQuestion.setAnswerField(answerField);
        return challengeQuestion;
    }

    private FieldType fieldType(String type) {
        return FieldType.builder().type(type).build();
    }

    private CaseDetails createCase() throws JsonProcessingException {
        Map<String, JsonNode> data = objectMapper.readValue(caseDataString(), new TypeReference<>() { });
        return CaseDetails.builder()
        .id("1")
        .data(data)
        .build();
    }

    private String caseDataString() {
        return "{\n"
            + "    \"DateField\": \"1985-07-25\",\n"
            + "    \"TextField\": \"TextValue\",\n"
            + "    \"ApplicantField\": \"\",\n"
            + "    \"Applicant2Field\": \"Name2\",\n"
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
