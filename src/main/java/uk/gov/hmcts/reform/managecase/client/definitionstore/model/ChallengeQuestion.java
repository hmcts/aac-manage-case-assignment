package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
public class ChallengeQuestion {

    @JsonProperty("case_type_id")
    private String caseTypeId;
    @JsonProperty("order")
    private Integer order;
    @JsonProperty("question_text")
    private String questionText;
    @JsonProperty("answer_field_type")
    private FieldType answerFieldType;
    @JsonProperty("display_context_parameter")
    private String displayContextParameter;
    @JsonProperty("challenge_question_id")
    private String challengeQuestionId;
    @JsonProperty("answer_field")
    private String answerField;
    @JsonProperty("question_id")
    private String questionId;

    @JsonIgnore
    private List<ChallengeAnswer> answers;

    @JsonSetter
    public void setAnswerField(String answerField) {
        this.answerField = answerField;
        initAnswersList();
    }

    private void initAnswersList() {
        answers = new ArrayList<>();
        String[] potentialAnswers = answerField.split(",");

        for (String potentialAnswer : potentialAnswers) {
            answers.add(new ChallengeAnswer(potentialAnswer));
        }
    }
}
