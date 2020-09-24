package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Data
public class ChallengeQuestion {

    private String caseTypeId;
    private Integer order;
    private String questionText;
    private FieldType answerFieldType;
    private String displayContextParameter;
    private String challengeQuestionId;
    private String answerField;
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
