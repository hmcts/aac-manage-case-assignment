package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class ChallengeAnswer {

    private List<String> fieldIds;
    private String caseRoleId;

    public ChallengeAnswer(String answerField) {
        String[] splitString = answerField.split(":");
        String[] fields = splitString[0].split("\\|");
        this.fieldIds = Arrays.stream(fields)
            .map(fieldId -> fieldId.substring(2, fieldId.length() - 1))
            .collect(Collectors.toList());
        this.caseRoleId = splitString[1];
    }
}
