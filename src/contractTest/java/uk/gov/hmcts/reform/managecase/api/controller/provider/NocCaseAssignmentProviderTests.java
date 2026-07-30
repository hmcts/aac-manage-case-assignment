package uk.gov.hmcts.reform.managecase.api.controller.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.reform.managecase.config.MapperConfig;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeQuestions;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
@Provider("acc_manageCaseAssignment_Noc")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
    host = "${PACT_BROKER_URL:localhost}",
    port = "${PACT_BROKER_PORT:80}")
@ContextConfiguration(classes = {ContractConfig.class, MapperConfig.class})
@IgnoreNoPactsToVerify
public class NocCaseAssignmentProviderTests {

    @Autowired
    NoticeOfChangeQuestions noticeOfChangeQuestions;

    @Autowired
    NoticeOfChangeController noticeOfChangeController;

    @PactBrokerConsumerVersionSelectors
    public static SelectorBuilder consumerVersionSelectors() {
        return new SelectorBuilder()
            .latestTag(System.getenv().getOrDefault("PACT_CONSUMER_TAG", "master"));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        //System.getProperties().setProperty("pact.verifier.publishResults", "true");
        testTarget.setControllers(noticeOfChangeController);
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @State("NoC questions exist for case with given id")
    public void toGetNoCQuestions() {
        given(noticeOfChangeQuestions.getChallengeQuestions(anyString()))
            .willReturn(ChallengeQuestionsResult.builder()
                .questions(List.of(
                    ChallengeQuestion.builder()
                        .caseTypeId("FT_NoCCaseType")
                        .order(6)
                        .questionText("What's the name of the party you wish to represent?")
                        .answerField("")
                        .answerFieldType(FieldType.builder().id("Text").type("Text")
                            .min("0").max("10").regularExpression("asdsa")
                            .fixedListItems(emptyList()).complexFields(emptyList()).build())
                        .challengeQuestionId("NoCChallenge")
                        .questionId("NoC_Challenge_Name")
                        .displayContextParameter("1")
                        .ignoreNullFields(true)
                        .build(),
                    ChallengeQuestion.builder()
                        .caseTypeId("FT_NoCCaseType")
                        .order(7)
                        .questionText("significant date?")
                        .answerField("")
                        .answerFieldType(FieldType.builder().id("Date").type("Date")
                            .min("0").max("10").regularExpression("asdsa")
                            .fixedListItems(emptyList()).complexFields(emptyList()).build())
                        .displayContextParameter("#DATETIMEENTRY(dd-MM-yyyy)")
                        .challengeQuestionId("NoCChallenge")
                        .questionId("NoC_Challenge_Name2")
                        .build()))
                .build());
    }
}
