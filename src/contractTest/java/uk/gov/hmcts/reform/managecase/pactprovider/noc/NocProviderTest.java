package uk.gov.hmcts.reform.managecase.pactprovider.noc;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeResponse;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersResponse;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.reform.managecase.domain.ApprovalStatus;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.pactprovider.noc.controller.NocRestController;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeQuestions;
import uk.gov.hmcts.reform.managecase.service.noc.RequestNoticeOfChangeService;
import uk.gov.hmcts.reform.managecase.service.noc.VerifyNoCAnswersService;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Provider("acc_manageCaseAssignment_Noc")
@PactBroker(
    url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "master")})
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
public class NocProviderTest {

    @Mock
    private NoticeOfChangeQuestions mockNoticeOfChangeQuestions;

    @Mock
    private VerifyNoCAnswersService mockVerifyNoCAnswersService;

    @Mock
    private RequestNoticeOfChangeService mockRequestNoticeOfChangeService;

    @Mock
    private NoCRequestDetails mockNoCRequestDetails;

    @BeforeEach
    void beforeCreate(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new NocRestController(mockNoticeOfChangeQuestions, mockVerifyNoCAnswersService,
                                                        mockRequestNoticeOfChangeService));
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

    @State("a case with id 1234567890123456 exists")
    public void getNoticeOfChangeQuestions1() {
        when(mockNoticeOfChangeQuestions.getChallengeQuestions(anyString())).thenReturn(mockChallengeQuestionsResult());
    }

    @State("a case with an error exists")
    public void getNoticeOfChangeQuestions2() {
    }

    @State("A valid submit NoC event is requested")
    public void requestNoticeOfChange1() {
        when(mockVerifyNoCAnswersService.verifyNoCAnswers(any(VerifyNoCAnswersRequest.class)))
            .thenReturn(mockNoCRequestDetails);
        when(mockRequestNoticeOfChangeService.requestNoticeOfChange(any(NoCRequestDetails.class)))
            .thenReturn(mockRequestNoticeOfChangeResponse());
    }

    @State("A NoC answer request with invalid case ID")
    public void requestNoticeOfChange2() {
        when(mockVerifyNoCAnswersService.verifyNoCAnswers(any(VerifyNoCAnswersRequest.class)))
            .thenReturn(mockNoCRequestDetails);
    }

    @State("A valid NoC answers verification request")
    public void verifyNoticeOfChangeAnswers1() {
        when(mockVerifyNoCAnswersService.verifyNoCAnswers(any(VerifyNoCAnswersRequest.class)))
            .thenReturn(mockNoCRequestDetails);
        when(mockNoCRequestDetails.toVerifyNoCAnswersResponse(anyString())).thenReturn(mockVerifyNoCAnswersResponse());
    }

    @State("An invalid NoC answer request")
    public void verifyNoticeOfChangeAnswers2() {
        when(mockVerifyNoCAnswersService.verifyNoCAnswers(any(VerifyNoCAnswersRequest.class)))
            .thenReturn(mockNoCRequestDetails);
    }


    private ChallengeQuestionsResult mockChallengeQuestionsResult() {
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

    private RequestNoticeOfChangeResponse mockRequestNoticeOfChangeResponse() {
        return RequestNoticeOfChangeResponse.builder()
            .approvalStatus(ApprovalStatus.APPROVED)
            .caseRole("[Claimant]")
            .status("Notice of request has been successfully submitted.")
            .build();
    }

    private VerifyNoCAnswersResponse mockVerifyNoCAnswersResponse() {
        return new VerifyNoCAnswersResponse("Notice of Change answers verified successfully",
                                            new Organisation("QUK822NA", "Some Org"));
    }
}
