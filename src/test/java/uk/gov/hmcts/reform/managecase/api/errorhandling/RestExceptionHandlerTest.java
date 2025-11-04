package uk.gov.hmcts.reform.managecase.api.errorhandling;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController;
import uk.gov.hmcts.reform.managecase.service.noc.ApplyNoCDecisionService;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeApprovalService;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeQuestions;
import uk.gov.hmcts.reform.managecase.service.noc.RequestNoticeOfChangeService;
import uk.gov.hmcts.reform.managecase.service.noc.PrepareNoCService;
import uk.gov.hmcts.reform.managecase.service.noc.VerifyNoCAnswersService;

import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.GET_NOC_QUESTIONS;

@DirtiesContext  // required for Jenkins agent
@AutoConfigureWireMock(port = 0)
public class RestExceptionHandlerTest {

    private MockMvc mockMvc;

    @Mock
    protected NoticeOfChangeQuestions service;

    @Mock
    protected NoticeOfChangeApprovalService approvalService;

    @Mock
    protected PrepareNoCService prepareNoCService;

    @Mock
    protected VerifyNoCAnswersService verifyNoCAnswersService;

    @Mock
    protected ApplyNoCDecisionService applyNoCDecisionService;

    @Mock
    protected RequestNoticeOfChangeService requestNoticeOfChangeService;

    @Mock
    protected JacksonUtils jacksonUtils;


    private static final String CASE_ID = "1567934206391385";


    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        NoticeOfChangeController controller = new NoticeOfChangeController(service,
                                                                           approvalService,
                                                                           verifyNoCAnswersService,
                                                                           prepareNoCService,
                                                                           requestNoticeOfChangeService,
                                                                           applyNoCDecisionService,
                                                                           jacksonUtils);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new RestExceptionHandler())
            .build();

    }

    @Test
    public void handleException_shouldReturnCaseCouldNotBeFoundResponse() throws Exception {

        // ARRANGE
        String myUniqueExceptionMessage = "Case could not be found";
        // any runtime exception (that is not an CaseAssignedUserRoleException)
        CaseCouldNotBeFoundException expectedException =
            new CaseCouldNotBeFoundException(myUniqueExceptionMessage);

        setupMockServiceToThrowException(expectedException);
        ResultActions result =  this.mockMvc.perform(get("/noc" + GET_NOC_QUESTIONS)
                                                         .queryParam("case_id", CASE_ID));

        assertHttpErrorResponse(result, expectedException);

    }

    private void setupMockServiceToThrowException(Exception expectedException) {
        // configure chosen mock service to throw exception when controller is run
        when(service.getChallengeQuestions(CASE_ID)).thenThrow(expectedException);
    }

    private void assertHttpErrorResponse(ResultActions result, Exception expectedException) throws Exception {

        result.andExpect(jsonPath("$.message").value(expectedException.getMessage()));
    }

}

