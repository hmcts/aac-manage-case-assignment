package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChallengeQuestionTest {

    @BeforeEach
    void setUp() {

    }

    @Test
    void test() {
        ChallengeQuestion challengeQuestion = new ChallengeQuestion();
        challengeQuestion.setAnswerField("${OrganisationField.OrganisationName}|${OrganisationField.OrganisationID}:"
            + "[Claimant],${Litigant2Field}:[Defendant]");

        System.out.println("test");
    }
}
