package uk.gov.hmcts.reform.managecase.api.controller.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController;
import uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController;
import uk.gov.hmcts.reform.managecase.api.errorhandling.RestExceptionHandler;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCException;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeResponse;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.reform.managecase.config.MapperConfig;
import uk.gov.hmcts.reform.managecase.domain.ApprovalStatus;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeQuestions;
import uk.gov.hmcts.reform.managecase.service.noc.RequestNoticeOfChangeService;
import uk.gov.hmcts.reform.managecase.service.noc.VerifyNoCAnswersService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.ANSWERS_NOT_MATCH_LITIGANT;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.MISSING_COR_CASE_ROLE;

@ExtendWith(SpringExtension.class)
@Provider("acc_manageCaseAssignment")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
    host = "${PACT_BROKER_URL:localhost}",
    port = "${PACT_BROKER_PORT:80}",
    consumerVersionSelectors = {@VersionSelector(tag = "master")})
@ContextConfiguration(classes = {ContractConfig.class, MapperConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DirtiesContext
public class CaseAssignmentExUIProviderTests {

    @Autowired
    NoticeOfChangeQuestions noticeOfChangeQuestions;

    @Autowired
    NoticeOfChangeController noticeOfChangeController;

    @Autowired
    CaseAssignmentController caseAssignmentController;

    @Autowired
    CaseAssignmentService caseAssignmentService;

    @Autowired
    VerifyNoCAnswersService verifyNoCAnswersService;

    @Autowired
    RequestNoticeOfChangeService requestNoticeOfChangeService;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        RestExceptionHandler exceptionHandler = new RestExceptionHandler();
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(noticeOfChangeController, caseAssignmentController);
        testTarget.setControllerAdvices(exceptionHandler);
        context.setTarget(testTarget);
    }

    @State("NoC questions exist for case with given id")
    public void givenNoCQuestionExistForCaseWithId(Map<String, Object> params) {
        String caseId = (String) params.get("caseId");
        given(noticeOfChangeQuestions.getChallengeQuestions(caseId)).willReturn(makeChallengeQuestionsResult());
    }

    @State({"Assign a user to a case"})
    public void givenAssignUserToCase() {
        given(caseAssignmentService.assignCaseAccess(any(CaseAssignment.class), anyBoolean()))
            .willReturn(List.of("Role1", "Role2"));
    }

    @State("A valid submit NoC event is requested")
    public void givenValidSubmitNoCEventRequested() {
        NoCRequestDetails details = NoCRequestDetails.builder().build();
        given(verifyNoCAnswersService.verifyNoCAnswers(any(VerifyNoCAnswersRequest.class)))
            .willReturn(details);
        RequestNoticeOfChangeResponse response = RequestNoticeOfChangeResponse.builder()
            .approvalStatus(ApprovalStatus.APPROVED)
            .caseRole("[Claimant]")
            .status(REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)
            .build();

        given(requestNoticeOfChangeService.requestNoticeOfChange(details))
            .willReturn(response);
    }

    @State("A NoC answer request with invalid case ID")
    public void givenNoCAnswerRequestWithInvalidCaseId() {
        NoCRequestDetails details = NoCRequestDetails.builder().build();
        given(verifyNoCAnswersService.verifyNoCAnswers(any(VerifyNoCAnswersRequest.class)))
            .willReturn(details);

        given(requestNoticeOfChangeService.requestNoticeOfChange(details))
            .willThrow(new NoCException((String.format(
                MISSING_COR_CASE_ROLE.getErrorMessage(), "[APPLICANT]")),
                                        MISSING_COR_CASE_ROLE.getErrorCode()));
    }

    @State("A valid NoC answers verification request")
    public void givenValidNoCAnswersVerificationRequest() {
        Organisation organisation = new Organisation("QUK822NA", "Some Org");
        NoCRequestDetails requestDetails = NoCRequestDetails.builder()
            .organisationPolicy(OrganisationPolicy.builder().organisation(organisation).build())
            .build();
        given(verifyNoCAnswersService.verifyNoCAnswers(any(VerifyNoCAnswersRequest.class)))
            .willReturn(requestDetails);
    }

    @State("An invalid NoC answer request")
    public void givenInvalidNoCAnswerRequest() {
        given(verifyNoCAnswersService.verifyNoCAnswers(any(VerifyNoCAnswersRequest.class)))
            .willThrow(new NoCException(ANSWERS_NOT_MATCH_LITIGANT));
    }

    private ChallengeQuestionsResult makeChallengeQuestionsResult() {
        return ChallengeQuestionsResult.builder()
            .questions(List.of(makeChallengeQuestion()))
            .build();
    }

    private ChallengeQuestion makeChallengeQuestion() {
        return ChallengeQuestion.builder()
            .caseTypeId("Probate")
            .order(1)
            .questionText("What is their Email?")
            .answerFieldType(makeFieldType())
            .displayContextParameter("1")
            .challengeQuestionId("NoC")
            .answerField("")
            .questionId("QuestionId67745")
            .build();
    }

    private FieldType makeFieldType() {
        return FieldType.builder()
            .id("Email")
            .type("Email")
            .min("0")
            .max("10")
            .regularExpression("asdsa")
            .build();
    }
}
