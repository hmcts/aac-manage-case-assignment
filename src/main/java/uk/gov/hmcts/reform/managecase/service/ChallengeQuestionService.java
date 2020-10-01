package uk.gov.hmcts.reform.managecase.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeAnswer;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.reform.managecase.domain.SubmittedChallengeAnswer;

import javax.validation.ValidationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.CharMatcher.anyOf;
import static com.google.common.base.CharMatcher.whitespace;

@Service
public class ChallengeQuestionService {

    public String getMatchingCaseRole(ChallengeQuestionsResult challengeQuestions,
                                      List<SubmittedChallengeAnswer> submittedAnswers,
                                      SearchResultViewItem caseResult) {
        validateNumberOfAnswers(submittedAnswers, challengeQuestions);

        Map<String, Integer> caseRoleCorrectAnswers =
            getCaseRoleCorrectAnswers(submittedAnswers, challengeQuestions, caseResult);

        return getMatchingCaseRoleId(caseRoleCorrectAnswers, challengeQuestions);
    }

    private Map<String, Integer> getCaseRoleCorrectAnswers(List<SubmittedChallengeAnswer> answers,
                                                           ChallengeQuestionsResult challengeQuestions,
                                                           SearchResultViewItem caseResult) {
        Map<String, Integer> caseRoleCorrectAnswers = new HashMap<>();
        challengeQuestions.getQuestions().forEach(question -> {
            String submittedAnswer = getSubmittedAnswer(answers, question.getQuestionId());

            question.getAnswers().forEach(answer -> {
                List<String> acceptedValues = answer.getFieldIds().stream()
                    .map(caseResult::getFieldValue)
                    .collect(Collectors.toList());

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
            throw new ValidationException(String.format(
                "The number of provided answers must match the number of questions - expected %s answers, received %s",
                noOfQuestions, noOfProvidedAnswers));
        }
    }

    private String getMatchingCaseRoleId(Map<String, Integer> caseRoleCorrectAnswers,
                                         ChallengeQuestionsResult challengeQuestions) {
        List<String> matchingCaseRoleIds = caseRoleCorrectAnswers.keySet().stream()
            .filter(caseRoleId -> caseRoleCorrectAnswers.get(caseRoleId) == challengeQuestions.getQuestions().size())
            .collect(Collectors.toList());

        if (matchingCaseRoleIds.isEmpty()) {
            throw new ValidationException("The answers did not match those for any litigant");
        }

        if (matchingCaseRoleIds.size() > 1) {
            throw new ValidationException("The answers did not uniquely identify a litigant");
        }

        return matchingCaseRoleIds.get(0);
    }

    private String getSubmittedAnswer(List<SubmittedChallengeAnswer> answers, String questionId) {
        return answers.stream()
            .filter(answer -> answer.getQuestionId().equals(questionId))
            .map(SubmittedChallengeAnswer::getValue)
            .findFirst()
            .orElseThrow(() -> new ValidationException(String.format(
                "No answer has been provided for question ID '%s'", questionId)));
    }

    private boolean isEqualAnswer(String expectedAnswer, String actualAnswer, FieldType fieldType) {
        if (expectedAnswer == null || actualAnswer == null) {
            return expectedAnswer == null && actualAnswer == null;
        }

        if (fieldType.getType().equals("Text")) {
            return formattedString(expectedAnswer).equalsIgnoreCase(formattedString(actualAnswer));
        }

        return expectedAnswer.equalsIgnoreCase(actualAnswer);
    }

    private String formattedString(String textValue) {
        return whitespace().or(anyOf("-'")).removeFrom(textValue);
    }
}
