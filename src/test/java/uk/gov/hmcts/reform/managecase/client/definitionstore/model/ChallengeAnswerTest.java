package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ChallengeAnswerTest {

    private static final String ANSWER_FIELD = "${applicant.individual.fullname}|${applicant.company.name}|"
        + "${applicant.soletrader.name}:Applicant";

    @Test
    @DisplayName("Splits String Challenge Answer up")
    void shouldReturnSeparatedChallengeAnswer() {
        ChallengeAnswer answer = new ChallengeAnswer(ANSWER_FIELD);
        assertThat(answer.getCaseRoleId()).isEqualTo("Applicant");
        assertThat(answer.getFieldIds().get(0)).isEqualTo("applicant.individual.fullname");
        assertThat(answer.getFieldIds().get(1)).isEqualTo("applicant.company.name");
        assertThat(answer.getFieldIds().get(2)).isEqualTo("applicant.soletrader.name");
    }
}
