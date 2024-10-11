package uk.gov.hmcts.reform.managecase.pactprovider.noc;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.reform.managecase.pactprovider.noc.controller.NocRestController;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeQuestions;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeApprovalService;
import uk.gov.hmcts.reform.managecase.service.noc.VerifyNoCAnswersService;
import uk.gov.hmcts.reform.managecase.service.noc.PrepareNoCService;
import uk.gov.hmcts.reform.managecase.service.noc.RequestNoticeOfChangeService;
import uk.gov.hmcts.reform.managecase.service.noc.ApplyNoCDecisionService;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/*
 * **** WORK IN PROGRESS ****
 * Three ways of running this Provider Test :-
 * [1] Set useProxyRestController = false                       -- Uses actual Rest Controller ,
 *                                                                 passes with changes to JSON below.
 * [2] Set useProxyRestController = true and activate method 1  -- Uses proxy  Rest Controller  ,  passes.
 * [3] Set useProxyRestController = true and activate method 2  -- Uses proxy  Rest Controller  ,
 *                                                                 passes with changes to JSON below.
 *
 * The PactFolder JSON file currently only contains ONE test (providerState "a case with id 1234567890123456 exist").
 *                                                  ^^^
 * Changes made to JSON file :-
 *     Updated order from "1" to 1                    -- reasonable because ChallengeQuestion order is Integer.
 *     Updated min & max from 0 & 10 to "0" & "10"    -- reasonable because FieldType min & max are String.
 *     Removed collection_field_type  (temporarily?)  -- because FieldType collectionFieldType is NOT String.
 *
 * Next thing to investigate:  LuhnCheck
 */

@Provider("acc_manageCaseAssignment_Noc")
@PactBroker(
    url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "master")})
@IgnoreNoPactsToVerify
@PactFolder("target/pacts/reform/acc_manageCaseAssignment_noc")
@ExtendWith(SpringExtension.class)
public class NocProviderTest {

    @Mock private NoticeOfChangeQuestions mockNoticeOfChangeQuestions;
    @Mock private NoticeOfChangeApprovalService mockNoticeOfChangeApprovalService;
    @Mock private VerifyNoCAnswersService mockVerifyNoCAnswersService;
    @Mock private PrepareNoCService mockPrepareNoCService;
    @Mock private RequestNoticeOfChangeService mockRequestNoticeOfChangeService;
    @Mock private ApplyNoCDecisionService mockApplyNoCDecisionService;
    @Mock private JacksonUtils mockJacksonUtils;

    private boolean useProxyRestController = true;

    @BeforeEach
    void beforeCreate(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        if (useProxyRestController) {
            // Either use PROXY Rest Controller :-
            testTarget.setControllers(new NocRestController(mockNoticeOfChangeQuestions));
        } else {
            // Or use ACTUAL Rest Controller :-
            testTarget.setControllers(new NoticeOfChangeController(mockNoticeOfChangeQuestions,
                                                                   mockNoticeOfChangeApprovalService,
                                                                   mockVerifyNoCAnswersService, mockPrepareNoCService,
                                                                   mockRequestNoticeOfChangeService,
                                                                   mockApplyNoCDecisionService, mockJacksonUtils));
        }
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    // Mock the interaction.
    @State("a case with id 1234567890123456 exists")
    public void getNoticeOfChangeQuestions1() {
        when(mockNoticeOfChangeQuestions.getChallengeQuestions(anyString())).thenReturn(mockChallengeQuestionsResult());
    }

    // Mock the interaction.
    @State("a case with an error exists")
    public void getNoticeOfChangeQuestions2() {
    }

    public ChallengeQuestionsResult mockChallengeQuestionsResult() {
        FieldType answerFieldType = FieldType.builder()
            .id("Email")
            .type("Email")
            .min("0")
            .max("10")
            .regularExpression("asdsa")
            .collectionFieldType(FieldType.builder().build())
            .build();
        ChallengeQuestion challengeQuestion = ChallengeQuestion.builder()
            .caseTypeId("Probate")
            .order(1)
            .questionText("What is their Email?")
            .answerFieldType(answerFieldType)
            .displayContextParameter("1")
            .challengeQuestionId("NoC")
            .questionId("QuestionId67745")
            .answerField("")
            .build();

        List<ChallengeQuestion> challengeQuestionsResponse = new ArrayList<>();
        challengeQuestionsResponse.add(challengeQuestion);

        return ChallengeQuestionsResult.builder().questions(challengeQuestionsResponse).build();
    }
}
