package uk.gov.hmcts.reform.managecase.service.noc;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCException;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeAnswer;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.reform.managecase.domain.SubmittedChallengeAnswer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.CharMatcher.anyOf;
import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.ANSWERS_NOT_IDENTIFY_LITIGANT;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.ANSWERS_NOT_MATCH_LITIGANT;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.NO_ANSWER_PROVIDED;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.ANSWER_MISMATCH_QUESTIONS;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.TEXT;

@Component
@SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.UseConcurrentHashMap"})
public class ChallengeAnswerValidator {

    public String getMatchingCaseRole(ChallengeQuestionsResult challengeQuestions,
                                      List<SubmittedChallengeAnswer> submittedAnswers,
                                      CaseDetails caseDetails) {
        validateNumberOfAnswers(submittedAnswers, challengeQuestions);

        Map<String, Integer> caseRoleCorrectAnswers =
            getCaseRoleCorrectAnswers(submittedAnswers, challengeQuestions, caseDetails);

        return getMatchingCaseRole(caseRoleCorrectAnswers, challengeQuestions);
    }

    private String getMatchingCaseRole(Map<String, Integer> caseRoleCorrectAnswers,
                                       ChallengeQuestionsResult challengeQuestions) {
        List<String> matchingCaseRoleIds = caseRoleCorrectAnswers.keySet().stream()
            .filter(caseRoleId -> caseRoleCorrectAnswers.get(caseRoleId) == challengeQuestions.getQuestions().size())
            .collect(toList());

        if (matchingCaseRoleIds.isEmpty()) {
            throw new NoCException(ANSWERS_NOT_MATCH_LITIGANT);
        }

        if (matchingCaseRoleIds.size() > 1) {
            throw new NoCException(ANSWERS_NOT_IDENTIFY_LITIGANT);
        }

        return matchingCaseRoleIds.get(0);
    }

    private Map<String, Integer> getCaseRoleCorrectAnswers(List<SubmittedChallengeAnswer> answers,
                                                           ChallengeQuestionsResult challengeQuestions,
                                                           CaseDetails caseDetails) {
        Map<String, Integer> caseRoleCorrectAnswers = new HashMap<>();
        challengeQuestions.getQuestions().forEach(question -> {
            String submittedAnswer = getSubmittedAnswerForQuestion(answers, question.getQuestionId()).getValue();

            question.getAnswers().forEach(answer -> {
                List<String> acceptedValues = answer.getFieldIds().stream()
                    .map(caseDetails::getFieldValue)
                    .collect(toList());

                if (isMatchingAnswerFound(question, submittedAnswer, acceptedValues)) {
                    incrementCorrectAnswerCount(caseRoleCorrectAnswers, answer);
                }
            });
        });
        return caseRoleCorrectAnswers;
    }

    private boolean isMatchingAnswerFound(ChallengeQuestion question,
                                          String submittedAnswer,
                                          List<String> acceptedValues) {
        return acceptedValues.stream()
            .anyMatch(value -> isEqualAnswer(value, submittedAnswer, question.getAnswerFieldType()));
    }

    private void incrementCorrectAnswerCount(Map<String, Integer> caseRoleCorrectAnswers, ChallengeAnswer answer) {
        caseRoleCorrectAnswers.merge(answer.getCaseRoleId(), 1, Integer::sum);
    }

    private void validateNumberOfAnswers(List<SubmittedChallengeAnswer> answers,
                                         ChallengeQuestionsResult challengeQuestions) {
        int noOfQuestions = challengeQuestions.getQuestions().size();
        int noOfProvidedAnswers = answers.size();
        if (noOfQuestions != noOfProvidedAnswers) {
            throw new NoCException((String.format(ANSWER_MISMATCH_QUESTIONS.getErrorMessage(), noOfQuestions,
                                                  noOfProvidedAnswers)), ANSWER_MISMATCH_QUESTIONS.getErrorCode());
        }
    }

    private SubmittedChallengeAnswer getSubmittedAnswerForQuestion(List<SubmittedChallengeAnswer> answers,
                                                                   String questionId) {
        return answers.stream()
            .filter(answer -> answer.getQuestionId().equals(questionId))
            .findFirst()
            .orElseThrow(() -> new NoCException((String.format(
                NO_ANSWER_PROVIDED.getErrorMessage(), questionId)),NO_ANSWER_PROVIDED.getErrorCode()));
    }

    private boolean isEqualAnswer(String expectedAnswer, String actualAnswer, FieldType fieldType) {
        if (isNullOrEmpty(expectedAnswer) || isNullOrEmpty(actualAnswer)) {
            return isNullOrEmpty(expectedAnswer) && isNullOrEmpty(actualAnswer);
        }

        if (fieldType.getType().equals(TEXT)) {
            return formattedString(expectedAnswer).equalsIgnoreCase(formattedString(actualAnswer));
        }

        return expectedAnswer.equalsIgnoreCase(actualAnswer);
    }

    private String formattedString(String textValue) {
        return whitespace().or(anyOf("-'")).removeFrom(textValue);
    }
}
