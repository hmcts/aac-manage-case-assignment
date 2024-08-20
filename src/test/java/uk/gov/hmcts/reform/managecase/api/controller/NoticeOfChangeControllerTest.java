package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import feign.FeignException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import uk.gov.hmcts.reform.managecase.TestIdamConfiguration;
import uk.gov.hmcts.reform.managecase.api.payload.ApplyNoCDecisionRequest;
import uk.gov.hmcts.reform.managecase.api.payload.CallbackRequest;
import uk.gov.hmcts.reform.managecase.api.payload.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeRequest;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeResponse;
import uk.gov.hmcts.reform.managecase.api.payload.SubmitCallbackResponse;
import uk.gov.hmcts.reform.managecase.api.payload.AboutToStartCallbackRequest;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.reform.managecase.config.MapperConfig;
import uk.gov.hmcts.reform.managecase.config.SecurityConfiguration;
import uk.gov.hmcts.reform.managecase.domain.ApprovalStatus;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.DynamicList;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.domain.SubmittedChallengeAnswer;
import uk.gov.hmcts.reform.managecase.security.JwtGrantedAuthoritiesConverter;
import uk.gov.hmcts.reform.managecase.service.noc.ApplyNoCDecisionService;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeApprovalService;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeQuestions;
import uk.gov.hmcts.reform.managecase.service.noc.RequestNoticeOfChangeService;
import uk.gov.hmcts.reform.managecase.service.noc.PrepareNoCService;
import uk.gov.hmcts.reform.managecase.service.noc.VerifyNoCAnswersService;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import jakarta.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyList;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.oneOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.APPLY_NOC_DECISION;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.caseDetails;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.defaultCaseDetails;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.CHECK_NOC_APPROVAL_DECISION_NOT_APPLIED_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.CHECK_NOC_APPROVAL_DECISION_APPLIED_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.CHECK_NOTICE_OF_CHANGE_APPROVAL_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.GET_NOC_QUESTIONS;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.NOC_PREPARE_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.SET_ORGANISATION_TO_REMOVE_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.VERIFY_NOC_ANSWERS;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.VERIFY_NOC_ANSWERS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_EMPTY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID_LENGTH;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CHALLENGE_QUESTION_ANSWERS_EMPTY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_DETAILS_REQUIRED;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert", "PMD.ExcessiveImports"})
public class NoticeOfChangeControllerTest {

    private static final String CASE_ID = "1588234985453946";
    private static final String CASE_TYPE_ID = "caseType";
    private static final String ANSWER_FIELD = "${applicant.individual.fullname}|${applicant.company.name}|"
        + "${applicant.soletrader.name}:Applicant,${respondent.individual.fullname}|${respondent.company.name}"
        + "|${respondent.soletrader.name}:Respondent";

    @WebMvcTest(controllers = NoticeOfChangeController.class,
        includeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = MapperConfig.class),
        excludeFilters = @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes =
            { SecurityConfiguration.class, JwtGrantedAuthoritiesConverter.class }))
    @AutoConfigureMockMvc(addFilters = false)
    @ImportAutoConfiguration(TestIdamConfiguration.class)
    static class BaseWebMvcTest {

        @Autowired
        protected MockMvc mockMvc;

        @MockBean
        protected NoticeOfChangeQuestions service;

        @MockBean
        protected NoticeOfChangeApprovalService approvalService;

        @MockBean
        protected PrepareNoCService prepareNoCService;

        @MockBean
        protected VerifyNoCAnswersService verifyNoCAnswersService;

        @MockBean
        protected ApplyNoCDecisionService applyNoCDecisionService;

        @MockBean
        protected RequestNoticeOfChangeService requestNoticeOfChangeService;

        @MockBean
        protected JacksonUtils jacksonUtils;

        @MockBean
        protected WebEndpointsSupplier webEndpointsSupplier;

        @MockBean
        protected ControllerEndpointsSupplier controllerEndpointsSupplier;

        @MockBean
        protected EndpointMediaTypes endpointMediaTypes;

        @MockBean
        protected CorsEndpointProperties corsEndpointProperties;

        @MockBean
        protected WebEndpointProperties webEndpointProperties;

        @MockBean
        protected WebMvcEndpointHandlerMapping webMvcEndpointHandlerMapping;

        @Autowired
        protected ObjectMapper objectMapper;
    }

    @Nested
    @DisplayName("GET /noc/noc-questions")
    @SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert", "PMD.ExcessiveImports"})
    class GetNoticeOfChangeQuestions {

        @Nested
        @DisplayName("GET /noc/noc-questions")
        class GetCaseAssignments extends BaseWebMvcTest {

            @DisplayName("happy path test without mockWebMVC")
            @Test
            void directCallHappyPath() {
                // created to avoid IDE warnings in controller class that function is never used
                // ARRANGE
                FieldType fieldType = FieldType.builder()
                    .max(null)
                    .min(null)
                    .id("Number")
                    .type("Number")
                    .build();
                ChallengeQuestion challengeQuestion = ChallengeQuestion.builder()
                    .caseTypeId(CASE_TYPE_ID)
                    .challengeQuestionId("QuestionId1")
                    .questionText("QuestionText1")
                    .answerFieldType(fieldType)
                    .answerField(ANSWER_FIELD)
                    .questionId("NoC").build();
                ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(
                    Arrays.asList(challengeQuestion));

                given(service.getChallengeQuestions(CASE_ID)).willReturn(challengeQuestionsResult);

                NoticeOfChangeController controller = new NoticeOfChangeController(service,
                                                                                   approvalService,
                                                                                   verifyNoCAnswersService,
                                                                                   prepareNoCService,
                                                                                   requestNoticeOfChangeService,
                                                                                   applyNoCDecisionService,
                                                                                   jacksonUtils);

                // ACT
                ChallengeQuestionsResult response = controller.getNoticeOfChangeQuestions(CASE_ID);

                // ASSERT
                assertThat(response).isNotNull();
            }

            @DisplayName("should successfully get NoC questions")
            @Test
            void shouldGetCaseAssignmentsForAValidRequest() throws Exception {
                FieldType fieldType = FieldType.builder()
                    .max(null)
                    .min(null)
                    .id("Number")
                    .type("Number")
                    .build();
                ChallengeQuestion challengeQuestion = ChallengeQuestion.builder()
                    .caseTypeId(CASE_TYPE_ID)
                    .challengeQuestionId("QuestionId1")
                    .questionText("QuestionText1")
                    .answerFieldType(fieldType)
                    .answerField(ANSWER_FIELD)
                    .questionId("NoC").build();
                ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(
                    Arrays.asList(challengeQuestion));


                given(service.getChallengeQuestions(CASE_ID)).willReturn(challengeQuestionsResult);

                this.mockMvc.perform(get("/noc" + GET_NOC_QUESTIONS).queryParam("case_id", CASE_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(APPLICATION_JSON_VALUE));
            }

            @DisplayName("should fail with 400 bad request when caseIds query param is not passed")
            @Test
            void shouldFailWithBadRequestWhenCaseIdsInGetAssignmentsIsNull() throws Exception {
                this.mockMvc.perform(get("/noc" + GET_NOC_QUESTIONS))
                    .andExpect(status().isBadRequest());
            }

            @DisplayName("should fail with 400 bad request when caseIds is empty")
            @Test
            void shouldFailWithBadRequestWhenCaseIdsInGetAssignmentsIsEmpty() throws Exception {

                this.mockMvc.perform(get("/noc" +  GET_NOC_QUESTIONS).queryParam("case_id", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code",
                        oneOf("case-id-empty", "case-id-invalid", "case-id-invalid-length")))
                    .andExpect(jsonPath("$.errors", hasItems("Case ID can not be empty")));
            }

            @DisplayName("should fail with 400 bad request when caseIds is malformed or invalid")
            @Test
            void shouldFailWithBadRequestWhenCaseIdsInGetAssignmentsIsMalformed() throws Exception {

                this.mockMvc.perform(get("/noc" +  GET_NOC_QUESTIONS)
                    .queryParam("case_id", "121324,%12345"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code",
                        oneOf("case-id-invalid", "case-id-invalid-length")))
                    .andExpect(jsonPath("$.errors",
                        hasItems("Case ID has to be 16-digits long", "Case ID has to be a valid 16-digit Luhn number")
                    ));
            }
        }
    }

    @Nested
    @DisplayName("GET /noc/verify-noc-answers")
    @SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert", "PMD.ExcessiveImports"})
    class VerifyNoticeOfChangeAnswers extends BaseWebMvcTest {

        private static final String ENDPOINT_URL = "/noc" + VERIFY_NOC_ANSWERS;

        private static final String QUESTION_ID = "QuestionId";
        private static final String ANSWER_VALUE = "Answer";

        private VerifyNoCAnswersRequest request;

        @BeforeEach
        void setUp() {
            request = new VerifyNoCAnswersRequest(CASE_ID,
                singletonList(new SubmittedChallengeAnswer(QUESTION_ID, ANSWER_VALUE)));
            NoCRequestDetails noCRequestDetails = NoCRequestDetails.builder()
                .organisationPolicy(OrganisationPolicy.builder()
                    .organisation(Organisation.builder().organisationID("OrganisationID").build())
                    .build())
                .build();
            given(verifyNoCAnswersService.verifyNoCAnswers(any(VerifyNoCAnswersRequest.class)))
                .willReturn(noCRequestDetails);
        }

        @DisplayName("should verify challenge answers successfully for a valid request")
        @Test
        void shouldVerifyChallengeAnswers() throws Exception {
            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status_message", is(VERIFY_NOC_ANSWERS_MESSAGE)));
        }

        @DisplayName("should delegate to service domain for a valid request")
        @Test
        void shouldDelegateToServiceDomain() throws Exception {
            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            ArgumentCaptor<VerifyNoCAnswersRequest> captor = ArgumentCaptor.forClass(VerifyNoCAnswersRequest.class);
            verify(verifyNoCAnswersService).verifyNoCAnswers(captor.capture());
            assertThat(captor.getValue().getCaseId()).isEqualTo(CASE_ID);
            assertThat(captor.getValue().getAnswers().size()).isEqualTo(1);
            assertThat(captor.getValue().getAnswers().get(0).getQuestionId()).isEqualTo(QUESTION_ID);
            assertThat(captor.getValue().getAnswers().get(0).getValue()).isEqualTo(ANSWER_VALUE);
        }

        @DisplayName("should fail with 400 bad request when case id is null")
        @Test
        void shouldFailWithBadRequestWhenCaseIdIsNull() throws Exception {
            request = new VerifyNoCAnswersRequest(null,
                singletonList(new SubmittedChallengeAnswer(QUESTION_ID, ANSWER_VALUE)));

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(CASE_ID_EMPTY)));
        }

        @DisplayName("should fail with 400 bad request when case id is an invalid Luhn number")
        @Test
        void shouldFailWithBadRequestWhenCaseIdIsInvalidLuhnNumber() throws Exception {
            request = new VerifyNoCAnswersRequest("123",
                singletonList(new SubmittedChallengeAnswer(QUESTION_ID, ANSWER_VALUE)));

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors", hasItem(CASE_ID_INVALID_LENGTH)))
                .andExpect(jsonPath("$.errors", hasItem(CASE_ID_INVALID)));
        }

        @DisplayName("should fail with 400 bad request when no challenge answers are provided")
        @Test
        void shouldFailWithBadRequestWhenSubmittedAnswersIsEmpty() throws Exception {
            request = new VerifyNoCAnswersRequest(CASE_ID, emptyList());

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(CHALLENGE_QUESTION_ANSWERS_EMPTY)));
        }
    }

    @Nested
    @DisplayName("POST /noc/noc-requests")
    @SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert", "PMD.ExcessiveImports"})
    class PostNoticeOfChangeRequest extends BaseWebMvcTest {

        private NoCRequestDetails noCRequestDetails;
        private RequestNoticeOfChangeResponse requestNoticeOfChangeResponse;
        private RequestNoticeOfChangeRequest requestNoticeOfChangeRequest;
        private List<SubmittedChallengeAnswer> submittedAnswerList;

        @BeforeEach
        public void setup() {
            noCRequestDetails = NoCRequestDetails.builder().build();
            requestNoticeOfChangeResponse = RequestNoticeOfChangeResponse.builder()
                .status(REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)
                .build();

            submittedAnswerList = List.of(new SubmittedChallengeAnswer("question", "value"));
            requestNoticeOfChangeRequest = new RequestNoticeOfChangeRequest(CASE_ID, submittedAnswerList);

            given(verifyNoCAnswersService.verifyNoCAnswers(any(VerifyNoCAnswersRequest.class)))
                .willReturn(noCRequestDetails);
            given(requestNoticeOfChangeService.requestNoticeOfChange(noCRequestDetails))
                .willReturn(requestNoticeOfChangeResponse);
        }

        @DisplayName("happy path test without mockWebMVC")
        @Test
        void directCallHappyPath() {
            // ARRANGE
            NoticeOfChangeController controller = new NoticeOfChangeController(service,
                                                                               approvalService,
                                                                               verifyNoCAnswersService,
                                                                               prepareNoCService,
                                                                               requestNoticeOfChangeService,
                                                                               applyNoCDecisionService,
                                                                               jacksonUtils);

            // ACT
            RequestNoticeOfChangeResponse response = controller.requestNoticeOfChange(requestNoticeOfChangeRequest);

            // ASSERT
            assertThat(response)
                .isNotNull()
                .isEqualTo(requestNoticeOfChangeResponse);
        }

        @DisplayName("should successfully get NoC request")
        @Test
        void shouldGetRequestNoticeOfChangeResponseForAValidRequest() throws Exception {
            this.mockMvc.perform(post("/noc" + REQUEST_NOTICE_OF_CHANGE_PATH)
                .contentType(APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(requestNoticeOfChangeRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status_message", is(REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)));
        }

        @DisplayName("should error if request NoC request body is empty")
        @Test
        void shouldFailWithBadRequestForEmptyBody() throws Exception {
            this.mockMvc.perform(post("/noc" + REQUEST_NOTICE_OF_CHANGE_PATH))
                .andExpect(status().isBadRequest());
        }

        @DisplayName("should error if request NoC case id is empty")
        @Test
        void shouldFailWithBadRequestForEmptyCaseId() throws Exception {
            requestNoticeOfChangeRequest = new RequestNoticeOfChangeRequest(null, submittedAnswerList);
            postCallShouldReturnBadRequestWithErrorMessage(requestNoticeOfChangeRequest, CASE_ID_EMPTY);
        }

        @DisplayName("should error if request NoC case id is invalid")
        @Test
        void shouldFailWithBadRequestForInvalidCaseId() throws Exception {
            requestNoticeOfChangeRequest = new RequestNoticeOfChangeRequest("1nva1l1d3", submittedAnswerList);
            postCallShouldReturnBadRequestWithErrorMessage(requestNoticeOfChangeRequest, CASE_ID_INVALID);
        }

        @DisplayName("should error if request NoC case id is invalid length")
        @Test
        void shouldFailWithBadRequestForInvalidCaseIdLength() throws Exception {
            requestNoticeOfChangeRequest = new RequestNoticeOfChangeRequest("12345", submittedAnswerList);
            postCallShouldReturnBadRequestWithErrorMessage(requestNoticeOfChangeRequest, CASE_ID_INVALID_LENGTH);
        }

        @DisplayName("should error if request NoC submitted answer list is empty")
        @Test
        void shouldFailWithBadRequestForEmptySubmittedAnswerList() throws Exception {
            requestNoticeOfChangeRequest = new RequestNoticeOfChangeRequest("12345", emptyList());
            postCallShouldReturnBadRequestWithErrorMessage(requestNoticeOfChangeRequest,
                                                           CHALLENGE_QUESTION_ANSWERS_EMPTY);
        }

        @DisplayName("should error if downstream access denied exception is thrown")
        @Test
        void shouldFailWithForbiddenResponseIfDownstreamThrowsAccessDenied() throws Exception {

            given(requestNoticeOfChangeService.requestNoticeOfChange(noCRequestDetails))
                    .willThrow(AccessDeniedException.class);

            this.mockMvc.perform(post("/noc" + REQUEST_NOTICE_OF_CHANGE_PATH)
                .contentType(APPLICATION_JSON_VALUE)
                .content(objectMapper.writeValueAsString(requestNoticeOfChangeRequest)))
                .andExpect(status().isForbidden());
        }

        @DisplayName("should error if downstream service throws an exception")
        @Test
        void shouldFailWithInternalServerErrorIfDownstreamServiceThrowsException() throws Exception {

            given(requestNoticeOfChangeService.requestNoticeOfChange(noCRequestDetails))
                    .willThrow(FeignException.class);

            this.mockMvc.perform(post("/noc" + REQUEST_NOTICE_OF_CHANGE_PATH)
                    .contentType(APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(requestNoticeOfChangeRequest)))
                    .andExpect(status().isBadGateway());
        }

        @DisplayName("should error if Exception thrown")
        @Test
        void shouldFailWithInternalServerErrorIfExceptionThrown() throws Exception {

            given(requestNoticeOfChangeService.requestNoticeOfChange(any()))
                .willThrow(new RuntimeException());

            this.mockMvc.perform(post("/noc" + REQUEST_NOTICE_OF_CHANGE_PATH)
                    .contentType(APPLICATION_JSON_VALUE)
                    .content(objectMapper.writeValueAsString(requestNoticeOfChangeRequest)))
                    .andExpect(status().isInternalServerError());
        }

        private void postCallShouldReturnBadRequestWithErrorMessage(
                                                            RequestNoticeOfChangeRequest requestNoticeOfChangeRequest,
                                                            String caseIdInvalid) throws Exception {

            this.mockMvc.perform(post("/noc" + REQUEST_NOTICE_OF_CHANGE_PATH)
                                     .contentType(APPLICATION_JSON_VALUE)
                                     .content(objectMapper.writeValueAsString(requestNoticeOfChangeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasItem(caseIdInvalid)));
        }
    }

    @Nested
    @DisplayName("POST /noc/check-noc-approval")
    @SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert", "PMD.ExcessiveImports"})
    class PostCheckNoticeOfChangeApproval extends BaseWebMvcTest {

        private CallbackRequest request;
        private CaseDetails caseDetails;
        private ChangeOrganisationRequest changeOrganisationRequest;

        private static final String ENDPOINT_URL = "/noc" + CHECK_NOTICE_OF_CHANGE_APPROVAL_PATH;

        @BeforeEach
        void setUp() {
            changeOrganisationRequest = ChangeOrganisationRequest.builder()
                .organisationToAdd(Organisation.builder().organisationID("123").build())
                .organisationToRemove(Organisation.builder().organisationID("789").build())
                .caseRoleId(DynamicList.builder().build())
                .requestTimestamp(LocalDateTime.now())
                .approvalStatus(null)
                .build();

            given(jacksonUtils.convertValue(any(JsonNode.class), any()))
                .willReturn(changeOrganisationRequest);
        }

        @DisplayName("happy path test without mockWebMVC")
        @Test
        void directCallHappyPath() {
            changeOrganisationRequest.setApprovalStatus("1");
            caseDetails =  caseDetails(changeOrganisationRequest);
            request = new CallbackRequest(null, null, caseDetails);
            NoticeOfChangeController controller =
                new NoticeOfChangeController(service,
                                             approvalService,
                                             verifyNoCAnswersService,
                                             prepareNoCService,
                                             requestNoticeOfChangeService,
                                             applyNoCDecisionService,
                                             jacksonUtils);

            SubmitCallbackResponse response = controller.checkNoticeOfChangeApproval(request);

            assertThat(response).isNotNull();
            assertThat(response.getConfirmationBody()).isEqualTo(CHECK_NOC_APPROVAL_DECISION_APPLIED_MESSAGE);
            assertThat(response.getConfirmationHeader()).isEqualTo(CHECK_NOC_APPROVAL_DECISION_APPLIED_MESSAGE);

            verify(approvalService).findAndTriggerNocDecisionEvent(CASE_ID);
        }

        @DisplayName("should return 200 status code if all data is valid (ApprovalStatus as a Number)")
        @Test
        void shouldCheckForNoCApprovalWithNumberForApprovalStatus() throws Exception {
            changeOrganisationRequest.setApprovalStatus("1");
            caseDetails =  caseDetails(changeOrganisationRequest);
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmation_header", is(CHECK_NOC_APPROVAL_DECISION_APPLIED_MESSAGE)))
                .andExpect(jsonPath("$.confirmation_body", is(CHECK_NOC_APPROVAL_DECISION_APPLIED_MESSAGE)));

            verify(approvalService).findAndTriggerNocDecisionEvent(CASE_ID);

        }

        @DisplayName("should return 200 status code if all data is valid (ApprovalStatus as a String)")
        @Test
        void shouldCheckForNoCApprovalWithStringForApprovalStatus() throws Exception {
            changeOrganisationRequest.setApprovalStatus("APPROVED");
            caseDetails =  caseDetails(changeOrganisationRequest);
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        }

        @DisplayName("should return 200 status code if ApprovalStatus is not equal to 1")
        @Test
        void shouldReturnSuccessfullyIfApprovalStatusIsNotApprovedCode() throws Exception {
            changeOrganisationRequest.setApprovalStatus(ApprovalStatus.PENDING.getValue());
            caseDetails =  caseDetails(changeOrganisationRequest);
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmation_header", is(CHECK_NOC_APPROVAL_DECISION_NOT_APPLIED_MESSAGE)))
                .andExpect(jsonPath("$.confirmation_body", is(CHECK_NOC_APPROVAL_DECISION_NOT_APPLIED_MESSAGE)));

            verify(approvalService, never()).findAndTriggerNocDecisionEvent(CASE_ID);
        }

        @DisplayName("should return 200 status code if ApprovalStatus is not equal to APPROVED")
        @Test
        void shouldReturnSuccessfullyIfApprovalStatusIsNotApproved() throws Exception {
            changeOrganisationRequest.setApprovalStatus("REJECTED");
            caseDetails =  caseDetails(changeOrganisationRequest);
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
            
            verify(approvalService, never()).findAndTriggerNocDecisionEvent(CASE_ID);
        }

        @DisplayName("should error if case reference in Case Details is empty")
        @Test
        void shouldFailIfCaseReferenceIsEmpty() throws Exception {
            caseDetails = defaultCaseDetails().id(null).build();
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(CASE_ID_EMPTY)));
        }

        @DisplayName("should error if case reference in Case Details is an invalid length")
        @Test
        void shouldFailIfCaseReferenceIsInvalidLength() throws Exception {
            caseDetails = defaultCaseDetails().id("16032064624").build();
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(CASE_ID_INVALID_LENGTH)));
        }

        @DisplayName("should error if case reference in Case Details is an invalid Luhn Number")
        @Test
        void shouldFailIfCaseReferenceIsInvalidLuhnNumber() throws Exception {
            caseDetails = defaultCaseDetails().id("1588234985453947").build();
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(CASE_ID_INVALID)));
        }

        @DisplayName("should error if changeOrganisationRequestField not found in Case Details")
        @Test
        void shouldFailIfChangeOrganisationRequestFieldNotFound() throws Exception {
            caseDetails = defaultCaseDetails().build();
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID)));

        }

        @DisplayName("should error if changeOrganisationRequestField is invalid")
        @Test
        void shouldFailIfChangeOrganisationRequestIsInvalid() throws Exception {
            caseDetails = caseDetails(changeOrganisationRequest);
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID)));
        }
    }

    @Nested
    @DisplayName("POST /noc/set-organisation-to-remove")
    @SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert", "PMD.ExcessiveImports"})
    class PostSetOrganisationToRemove extends BaseWebMvcTest {

        private CallbackRequest request;
        private AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse;
        private CaseDetails caseDetails;
        private ChangeOrganisationRequest changeOrganisationRequest;
        private OrganisationPolicy organisationPolicy;
        private Organisation organisation;

        private static final String ENDPOINT_URL = "/noc" + SET_ORGANISATION_TO_REMOVE_PATH;

        @BeforeEach
        void setUp() {
            changeOrganisationRequest = ChangeOrganisationRequest.builder()
                .organisationToAdd(Organisation.builder().organisationID("123").build())
                .organisationToRemove(Organisation.builder().organisationID(null).build())
                .caseRoleId(DynamicList.builder().build())
                .requestTimestamp(LocalDateTime.now())
                .approvalStatus("1")
                .build();

            organisation = Organisation.builder()
                .organisationID("Org1")
                .build();

            organisationPolicy = OrganisationPolicy.builder()
                .organisation(organisation)
                .orgPolicyReference("PolicyRef")
                .orgPolicyCaseAssignedRole("Role1")
                .build();

            given(jacksonUtils.convertValue(any(JsonNode.class), any()))
                .willReturn(changeOrganisationRequest);
        }

        @DisplayName("happy path test without mockWebMVC")
        @Test
        void directCallHappyPath() {
            aboutToSubmitCallbackResponse = AboutToSubmitCallbackResponse.builder()
                .data(Map.of(
                    "OrganisationPolicyField1",
                    objectMapper.convertValue(organisationPolicy, JsonNode.class),
                    "ChangeOrganisationRequestField",
                    objectMapper.convertValue(changeOrganisationRequest, JsonNode.class))
                )
                .build();
            caseDetails = caseDetails(changeOrganisationRequest, organisationPolicy);

            given(requestNoticeOfChangeService.setOrganisationToRemove(any(), any(), any()))
                .willReturn(aboutToSubmitCallbackResponse);

            request = new CallbackRequest(null, null, caseDetails);

            NoticeOfChangeController controller =
                new NoticeOfChangeController(service,
                                             approvalService,
                                             verifyNoCAnswersService,
                                             prepareNoCService,
                                             requestNoticeOfChangeService,
                                             applyNoCDecisionService,
                                             jacksonUtils);

            AboutToSubmitCallbackResponse response = controller.setOrganisationToRemove(request);

            assertThat(response)
                .isNotNull()
                .isEqualTo(aboutToSubmitCallbackResponse);
        }

        @DisplayName("should return 200 status code if all constraints met")
        @Test
        void shouldSetOrganisationToRemove() throws Exception {
            ChangeOrganisationRequest updatedCOR = ChangeOrganisationRequest.builder()
                .organisationToAdd(Organisation.builder().organisationID("123").build())
                .organisationToRemove(Organisation.builder().organisationID("234").build())
                .caseRoleId(DynamicList.builder().build())
                .requestTimestamp(LocalDateTime.now())
                .approvalStatus("1")
                .build();

            aboutToSubmitCallbackResponse = AboutToSubmitCallbackResponse.builder()
                .data(Map.of(
                    "OrganisationPolicyField1",
                    objectMapper.convertValue(organisationPolicy, JsonNode.class),
                    "ChangeOrganisationRequestField",
                    objectMapper.convertValue(updatedCOR, JsonNode.class))
                )
                .build();

            caseDetails = caseDetails(changeOrganisationRequest, organisationPolicy);

            given(requestNoticeOfChangeService.setOrganisationToRemove(any(), any(), any()))
                .willReturn(aboutToSubmitCallbackResponse);

            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.OrganisationToRemove.OrganisationID",
                                    is("234")));
        }

        @DisplayName("should error if case reference in Case Details is empty")
        @Test
        void shouldFailIfCaseReferenceIsEmpty() throws Exception {
            caseDetails = defaultCaseDetails().id(null).build();
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(CASE_ID_EMPTY)));
        }

        @DisplayName("should error if case reference in Case Details is an invalid length")
        @Test
        void shouldFailIfCaseReferenceIsInvalidLength() throws Exception {
            caseDetails = defaultCaseDetails().id("16032064624").build();
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(CASE_ID_INVALID_LENGTH)));
        }

        @DisplayName("should error if case reference in Case Details is an invalid Luhn Number")
        @Test
        void shouldFailIfCaseReferenceIsInvalidLuhnNumber() throws Exception {
            caseDetails = defaultCaseDetails().id("1588234985453947").build();
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(CASE_ID_INVALID)));
        }

        @DisplayName("should error if changeOrganisationRequestField not found in Case Details")
        @Test
        void shouldFailIfChangeOrganisationRequestFieldNotFound() throws Exception {
            caseDetails = defaultCaseDetails().data(Map.of()).build();
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID)));
        }

        @DisplayName("should error if organisationToRemove fields invalid")
        @Test
        void shouldFailIfOrganisationToRemoveFieldsInvalid() throws Exception {
            changeOrganisationRequest.setOrganisationToRemove(organisation);
            caseDetails = caseDetails(changeOrganisationRequest, organisationPolicy);
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID)));
        }

        @DisplayName("should error if changeOrganisationRequestField is invalid")
        @Test
        void shouldFailIfChangeOrganisationRequestIsInvalid() throws Exception {
            changeOrganisationRequest.setApprovalStatus(null);
            caseDetails = caseDetails(changeOrganisationRequest);
            request = new CallbackRequest(null, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID)));
        }
    }

    @Nested
    @DisplayName("GET /noc/noc-prepare")
    @SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert", "PMD.ExcessiveImports"})
    class PrepareNoticeOfChangeEvent extends BaseWebMvcTest {

        private static final String ENDPOINT_URL = "/noc" + NOC_PREPARE_PATH;

        private AboutToStartCallbackRequest request;

        private final Map<String, JsonNode> responseData = new ConcurrentHashMap<>();

        @BeforeEach
        void setUp() throws Exception {
            request = new AboutToStartCallbackRequest("createEvent", null, CaseDetails.builder().id(CASE_ID).build());
            ChangeOrganisationRequest cor = ChangeOrganisationRequest.builder().build();

            responseData.put("ChangeOrganisationRequest", objectMapper.readTree(objectMapper.writeValueAsString(cor)));

            given(prepareNoCService.prepareNoCRequest(any(CaseDetails.class)))
                .willReturn(responseData);
        }

        @DisplayName("should verify a valid prepareNoCRequest")
        @Test
        void shouldPrepareNoCRequest() throws Exception {
            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(content().string("{\"data\":{\"ChangeOrganisationRequest\":"
                                                + "{\"OrganisationToAdd\":null,"
                                                + "\"OrganisationToRemove\":null,"
                                                + "\"CaseRoleId\":null,"
                                                + "\"RequestTimestamp\":null,"
                                                + "\"ApprovalStatus\":null,"
                                                + "\"CreatedBy\":null}},"
                                                + "\"state\":null,"
                                                + "\"errors\":null,"
                                                + "\"warnings\":null,"
                                                + "\"data_classification\":null,"
                                                + "\"security_classification\":null,"
                                                + "\"significant_item\":null"
                                                + "}"));
        }
    }

    @Nested
    @DisplayName("GET /noc/apply-decision")
    @SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert", "PMD.ExcessiveImports"})
    class ApplyNoticeOfChangeDecision extends BaseWebMvcTest {

        private static final String ENDPOINT_URL = "/noc" + APPLY_NOC_DECISION;

        private static final String FIELD_VALUE = "FieldValue";
        private static final String FIELD_ID = "FieldId";

        private ApplyNoCDecisionRequest request;

        @BeforeEach
        void setUp() {
            Map<String, JsonNode> data = new HashMap<>();
            data.put(FIELD_ID, new TextNode(FIELD_VALUE));
            request = new ApplyNoCDecisionRequest(CaseDetails.builder()
                .id(CASE_ID)
                .caseTypeId(CASE_TYPE_ID)
                .data(data)
                .build());
            given(applyNoCDecisionService.applyNoCDecision(any(ApplyNoCDecisionRequest.class))).willReturn(data);
        }

        @DisplayName("should apply notice of change decision successfully for a valid request")
        @Test
        void shouldApplyNoticeOfChangeDecision() throws Exception {
            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.data.length()", is(1)))
                .andExpect(jsonPath("$.data['FieldId']", is(FIELD_VALUE)))
                .andExpect(jsonPath("$.errors").doesNotExist());
        }

        @DisplayName("should accept valid request which includes extra unknown fields")
        @Test
        void shouldApplyNoticeOfChangeDecisionWithExtraUnknownField() throws Exception {
            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"case_details\": {}, \"extra_field\": \"value\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.data.length()", is(1)))
                .andExpect(jsonPath("$.data['FieldId']", is(FIELD_VALUE)))
                .andExpect(jsonPath("$.errors").doesNotExist());
        }

        @DisplayName("should delegate to service domain for a valid request")
        @Test
        void shouldDelegateToServiceDomain() throws Exception {
            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

            ArgumentCaptor<ApplyNoCDecisionRequest> captor = ArgumentCaptor.forClass(ApplyNoCDecisionRequest.class);
            verify(applyNoCDecisionService).applyNoCDecision(captor.capture());
            assertThat(captor.getValue().getCaseDetails().getId()).isEqualTo(CASE_ID);
            assertThat(captor.getValue().getCaseDetails().getCaseTypeId()).isEqualTo(CASE_TYPE_ID);
            assertThat(captor.getValue().getCaseDetails().getData().get(FIELD_ID).asText()).isEqualTo(FIELD_VALUE);
        }

        @DisplayName("should fail with 400 bad request when case details is null")
        @Test
        void shouldFailWithBadRequestWhenCaseIdIsNull() throws Exception {
            request = new ApplyNoCDecisionRequest(null);

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(CASE_DETAILS_REQUIRED)));
        }

        @DisplayName("should return 200 with errors array when handled exception occurs")
        @Test
        void shouldReturnSuccessResponseWithErrorsArrayForHandledExceptions() throws Exception {
            String errorMessage = "Error message value";
            doThrow(new ValidationException(errorMessage))
                .when(applyNoCDecisionService).applyNoCDecision(any(ApplyNoCDecisionRequest.class));

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors", hasItem(errorMessage)))
                .andExpect(jsonPath("$.data").doesNotExist());
        }
    }
}
