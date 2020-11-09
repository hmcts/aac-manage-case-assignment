package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.hamcrest.core.Is.is;

@SuppressWarnings("PMD.JUnitAssertionsShouldIncludeMessage")
class ChallengeQuestionTest {

    @Test
    void shouldSetAnswersList() {
        ChallengeQuestion challengeQuestion = ChallengeQuestion.builder().build();
        challengeQuestion.setAnswerField("${OrganisationField.OrganisationName}|${OrganisationField.OrganisationID}:"
            + "[Claimant],${Litigant2Field}:[Defendant]");

        assertAll(
            () -> assertThat(challengeQuestion.getAnswers().size(), is(2)),
            () -> assertThat(challengeQuestion.getAnswers().get(0).getCaseRoleId(), is("[Claimant]")),
            () -> assertThat(challengeQuestion.getAnswers().get(0).getFieldIds().size(), is(2)),
            () -> assertThat(challengeQuestion.getAnswers().get(0).getFieldIds().get(0),
                is("OrganisationField.OrganisationName")),
            () -> assertThat(challengeQuestion.getAnswers().get(0).getFieldIds().get(1),
                is("OrganisationField.OrganisationID")),
            () -> assertThat(challengeQuestion.getAnswers().get(1).getCaseRoleId(), is("[Defendant]")),
            () -> assertThat(challengeQuestion.getAnswers().get(1).getFieldIds().size(), is(1)),
            () -> assertThat(challengeQuestion.getAnswers().get(1).getFieldIds().get(0),
                is("Litigant2Field"))
        );
    }
}
