package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import uk.gov.hmcts.reform.managecase.BaseIT;
import uk.gov.hmcts.reform.managecase.api.payload.AboutToStartCallbackRequest;
import uk.gov.hmcts.reform.managecase.api.payload.ApplyNoCDecisionRequest;
import uk.gov.hmcts.reform.managecase.api.payload.CallbackRequest;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeRequest;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseEventCreationPayload;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.client.datastore.Event;
import uk.gov.hmcts.reform.managecase.client.datastore.StartEventResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseUpdateViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewActionableEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewJurisdiction;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewType;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.reform.managecase.domain.ApprovalStatus;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.DynamicList;
import uk.gov.hmcts.reform.managecase.domain.DynamicListElement;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.domain.SubmittedChallengeAnswer;
import uk.gov.hmcts.reform.managecase.repository.CaseTypeDefinitionVersion;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeApprovalService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.caseDetails;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.defaultCaseDetails;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseUpdateViewEventFixture.getCaseViewFields;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseUpdateViewEventFixture.getWizardPages;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.user;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.usersByOrganisation;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.APPLY_NOC_DECISION;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.CHECK_NOC_APPROVAL_DECISION_APPLIED_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.CHECK_NOC_APPROVAL_DECISION_NOT_APPLIED_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.CHECK_NOTICE_OF_CHANGE_APPROVAL_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.GET_NOC_QUESTIONS;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.NOC_PREPARE_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.SET_ORGANISATION_TO_REMOVE_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.VERIFY_NOC_ANSWERS;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.VERIFY_NOC_ANSWERS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_DETAILS_REQUIRED;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.INVALID_CASE_ROLE_FIELD;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.MULTIPLE_NOC_REQUEST_EVENTS;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_EVENT_NOT_AVAILABLE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_REQUEST_NOT_CONSIDERED;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_DATA_PROVIDED;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.APPROVED;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.PENDING;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.REJECTED;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseAssignments;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseInternal;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseInternalAsApprover;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseRoles;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseViaExternalApi;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetChallengeQuestions;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetLatestVersion;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetExternalStartEventTrigger;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetExternalStartEventTriggerAsApprover;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetStartEventTrigger;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetUsersByOrganisationInternal;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubSubmitEventForCase;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubUnassignCase;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.MethodNamingConventions",
    "PMD.AvoidDuplicateLiterals", "PMD.ExcessiveImports", "PMD.TooManyMethods", "PMD.UseConcurrentHashMap",
    "PMD.DataflowAnomalyAnalysis", "squid:S100", "squid:S1192"})
public class NoticeOfChangeControllerIT {

    private static final String CASE_ID = "1588234985453946";
    private static final String CASE_TYPE_ID = "caseType";
    private static final String JURISDICTION = "Jurisdiction";

    private static final String QUESTION_ID_1 = "QuestionId1";
    private static final String ORGANISATION_ID = "QUK822N";
    private static final String ORGANISATION_NAME = "CCD Solicitors Limited";
    private final Map<String, JsonNode> caseFields = new HashMap<>();
    private static final String ANSWER_FIELD_APPLICANT = "${applicant.individual.fullname}|${applicant.company.name}|"
        + "${applicant.soletrader.name}|${OrganisationPolicy1"
        + ".Organisation.OrganisationID}:Applicant";
    private static final String NOC = "NOC";

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws JsonProcessingException {
        mapper.registerModule(new JavaTimeModule());
        CaseViewActionableEvent caseViewActionableEvent = new CaseViewActionableEvent();
        caseViewActionableEvent.setId(NOC);
        CaseViewResource caseViewResource = new CaseViewResource();
        caseViewResource.setReference(CASE_ID);

        caseViewResource.setCaseViewActionableEvents(new CaseViewActionableEvent[] {caseViewActionableEvent});
        CaseViewType caseViewType = new CaseViewType();
        caseViewType.setId(CASE_TYPE_ID);
        CaseViewJurisdiction caseViewJurisdiction = new CaseViewJurisdiction();
        caseViewJurisdiction.setId(JURISDICTION);
        caseViewType.setJurisdiction(caseViewJurisdiction);
        caseViewResource.setCaseType(caseViewType);
        stubGetCaseInternal(CASE_ID, caseViewResource);

        FieldType fieldType = FieldType.builder()
            .regularExpression("regular expression")
            .max(null)
            .min(null)
            .id("Number")
            .type("Number")
            .build();
        ChallengeQuestion challengeQuestion = ChallengeQuestion.builder()
            .caseTypeId(CASE_TYPE_ID)
            .challengeQuestionId("NoC")
            .questionText("questionText")
            .answerFieldType(fieldType)
            .answerField(ANSWER_FIELD_APPLICANT)
            .questionId("QuestionId1")
            .order(1).build();
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(
            Arrays.asList(challengeQuestion));
        stubGetChallengeQuestions(CASE_TYPE_ID, "NoCChallenge", challengeQuestionsResult);

        CaseTypeDefinitionVersion caseTypeDefinitionVersion = new CaseTypeDefinitionVersion();
        caseTypeDefinitionVersion.setVersion(3);
        stubGetLatestVersion(CASE_TYPE_ID, caseTypeDefinitionVersion);

        List<CaseRole> caseRoleList = Arrays.asList(
            CaseRole.builder().id("[CLAIMANT]").name("Claimant").description("Claimant").build(),
            CaseRole.builder().id("[DEFENDANT]").name("Defendant").description("Defendant").build(),
            CaseRole.builder().id("[OTHER]").name("Other").description("Other").build()
        );
        stubGetCaseRoles("0", JURISDICTION, CASE_TYPE_ID, caseRoleList);

        Organisation organisationToAdd = Organisation.builder().organisationID("QUK822N").build();
        ChangeOrganisationRequest cor = ChangeOrganisationRequest.builder()
            .organisationToAdd(organisationToAdd)
            .createdBy("CreatedByUser")
            .build();

        OrganisationPolicy organisationPolicy = OrganisationPolicy.builder()
            .orgPolicyCaseAssignedRole("Applicant")
            .orgPolicyReference("ApplicantPolicy")
            .organisation(new Organisation(ORGANISATION_ID, ORGANISATION_NAME))
            .build();

        caseFields.put("ChangeOrgRequest", mapper.convertValue(cor, JsonNode.class));
        caseFields.put("OrganisationPolicy1", mapper.convertValue(organisationPolicy, JsonNode.class));

        CaseDetails caseDetails = CaseDetails.builder()
            .id(CASE_ID)
            .caseTypeId(CASE_TYPE_ID)
            .jurisdiction(JURISDICTION)
            .data(caseFields)
            .build();

        stubGetCaseViaExternalApi(CASE_ID, caseDetails);
    }

    @Nested
    @DisplayName("GET /noc/noc-questions")
    class GetNoticeOfChangeQuestions extends BaseIT {

        @DisplayName("Successfully return NoC questions for case id")
        @Test
        void shouldGetNoCQuestions_forAValidRequest() throws Exception {
            this.mockMvc.perform(get("/noc" + GET_NOC_QUESTIONS).queryParam("case_id", CASE_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.questions", hasSize(1)))
                .andExpect(jsonPath("$.questions[0].case_type_id", is(CASE_TYPE_ID)))
                .andExpect(jsonPath("$.questions[0].order", is(1)))
                .andExpect(jsonPath("$.questions[0].question_text", is("questionText")))
                .andExpect(jsonPath("$.questions[0].challenge_question_id", is("NoC")));

        }

        @DisplayName("Must return 400 bad request response if caseIds are missing in GetAssignments request")
        @Test
        void shouldReturn400_whenCaseIdsAreNotPassedForGetNoCQuestionsApi() throws Exception {

            this.mockMvc.perform(get("/noc" + GET_NOC_QUESTIONS).queryParam("case_id", ""))
                .andExpect(status().isBadRequest());
        }

        @DisplayName("Must return 400 bad request when no Noc event is available in the case")
        @Test
        void shouldReturnErrorWhenNoCEventIsNotAvailable() throws Exception {
            CaseViewResource caseViewResource = new CaseViewResource();
            caseViewResource.setCaseViewActionableEvents(new CaseViewActionableEvent[0]);
            stubGetCaseInternal(CASE_ID, caseViewResource);

            this.mockMvc.perform(get("/noc" + GET_NOC_QUESTIONS)
                .queryParam("case_id", CASE_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(NOC_EVENT_NOT_AVAILABLE)));
        }

        @DisplayName("Must return 400 bad request when multiple Noc events are available in the case")
        @Test
        void shouldReturnErrorWhenMultipleNoCEventsAreAvailable() throws Exception {
            CaseViewResource caseViewResource = new CaseViewResource();
            caseViewResource.setCaseViewActionableEvents(
                new CaseViewActionableEvent[]{new CaseViewActionableEvent(), new CaseViewActionableEvent()}
            );
            stubGetCaseInternal(CASE_ID, caseViewResource);

            this.mockMvc.perform(get("/noc" + GET_NOC_QUESTIONS)
                .queryParam("case_id", CASE_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(MULTIPLE_NOC_REQUEST_EVENTS)));
        }

    }

    @Nested
    @DisplayName("POST /noc/verify-noc-answers")
    class VerifyNoticeOfChangeQuestions extends BaseIT {

        private static final String ENDPOINT_URL = "/noc" + VERIFY_NOC_ANSWERS;

        @Test
        void shouldVerifyNoCAnswersSuccessfully() throws Exception {
            SubmittedChallengeAnswer answer = new SubmittedChallengeAnswer(QUESTION_ID_1,
                ORGANISATION_ID.toLowerCase(Locale.getDefault()));
            VerifyNoCAnswersRequest request = new VerifyNoCAnswersRequest(CASE_ID, singletonList(answer));

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status_message", is(VERIFY_NOC_ANSWERS_MESSAGE)))
                .andExpect(jsonPath("$.organisation.OrganisationID", is(ORGANISATION_ID)))
                .andExpect(jsonPath("$.organisation.OrganisationName", is("CCD Solicitors Limited")));
        }

        @Test
        void shouldReturnErrorForIncorrectAnswer() throws Exception {
            SubmittedChallengeAnswer answer = new SubmittedChallengeAnswer(QUESTION_ID_1, "Incorrect Answer");
            VerifyNoCAnswersRequest request = new VerifyNoCAnswersRequest(CASE_ID, singletonList(answer));

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.message", is("The answers did not match those for any litigant")));
        }

        @Test
        void shouldReturnErrorWhenExpectedQuestionIdIsNotPassed() throws Exception {
            SubmittedChallengeAnswer answer = new SubmittedChallengeAnswer("UnknownID", ORGANISATION_ID);
            VerifyNoCAnswersRequest request = new VerifyNoCAnswersRequest(CASE_ID, singletonList(answer));

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.message", is("No answer has been provided for question ID 'QuestionId1'")));
        }
    }

    @Nested
    @DisplayName("POST /noc/apply-decision")
    class ApplyNoticeOfChangeDecision extends BaseIT {

        private static final String ENDPOINT_URL = "/noc" + APPLY_NOC_DECISION;

        private static final String ORG_1_ID = "Org1Id";
        private static final String ORG_2_ID = "Org2Id";
        private static final String ORG_1_NAME = "Org1Name";
        private static final String ORG_2_NAME = "Org2Name";
        private static final String ORG_POLICY_1_REF = "DefendantPolicy";
        private static final String ORG_POLICY_2_REF = "ClaimantPolicy";
        private static final String ORG_POLICY_1_ROLE = "[Defendant]";
        private static final String ORG_POLICY_2_ROLE = "[Claimant]";
        private static final String USER_ID_1 = "UserId1";
        private static final String USER_ID_2 = "UserId2";

        @BeforeEach
        void setUp() throws JsonProcessingException {
            CaseUserRole caseUserRole = CaseUserRole.builder()
                .caseId(CASE_ID)
                .userId(USER_ID_2)
                .caseRole(ORG_POLICY_2_ROLE)
                .build();
            stubGetCaseAssignments(singletonList(CASE_ID), null, singletonList(caseUserRole));
            stubGetUsersByOrganisationInternal(usersByOrganisation(user(USER_ID_1), user(USER_ID_2)), ORG_2_ID);
            stubUnassignCase(singletonList(
                new CaseUserRoleWithOrganisation(CASE_ID, USER_ID_2, ORG_POLICY_2_ROLE, ORG_2_ID)));
        }

        @Test
        void shouldApplyNoCDecisionSuccessfullyWhenApproved() throws Exception {
            ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(CaseDetails.builder()
                .id(CASE_ID)
                .caseTypeId(CASE_TYPE_ID)
                .data(createData(APPROVED))
                .createdDate(LocalDateTime.now())
                .build());

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.Reason").isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.CaseRoleId").isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.NotesReason").isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.ApprovalStatus").isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.RequestTimestamp").isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.ApprovalStatus").isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.ApprovalRejectionTimestamp").isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.OrganisationToAdd.OrganisationID")
                    .isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.OrganisationToAdd.OrganisationName")
                    .isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.OrganisationToRemove.OrganisationID")
                    .isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.OrganisationToRemove.OrganisationName")
                    .isEmpty())
                .andExpect(jsonPath("$.data.OrganisationPolicyField1.Organisation.OrganisationID", is(ORG_1_ID)))
                .andExpect(jsonPath("$.data.OrganisationPolicyField1.Organisation.OrganisationName", is(ORG_1_NAME)))
                .andExpect(jsonPath("$.data.OrganisationPolicyField1.OrgPolicyReference", is(ORG_POLICY_1_REF)))
                .andExpect(jsonPath("$.data.OrganisationPolicyField1.OrgPolicyCaseAssignedRole", is(ORG_POLICY_1_ROLE)))
                .andExpect(jsonPath("$.data.OrganisationPolicyField2.Organisation.OrganisationID").isEmpty())
                .andExpect(jsonPath("$.data.OrganisationPolicyField2.Organisation.OrganisationName").isEmpty())
                .andExpect(jsonPath("$.data.OrganisationPolicyField2.PreviousOrganisations[0].value.OrganisationName",
                                    is(ORG_2_NAME)))
                .andExpect(jsonPath("$.data.OrganisationPolicyField2.PreviousOrganisations[0].value.FromTimestamp")
                               .isNotEmpty())
                .andExpect(jsonPath("$.data.OrganisationPolicyField2.PreviousOrganisations[0].value.ToTimestamp")
                               .isNotEmpty())
                .andExpect(jsonPath("$.data.OrganisationPolicyField2.OrgPolicyReference", is(ORG_POLICY_2_REF)))
                .andExpect(jsonPath("$.data.OrganisationPolicyField2.OrgPolicyCaseAssignedRole", is(ORG_POLICY_2_ROLE)))
                .andExpect(jsonPath("$.data.TextField", is("TextFieldValue")))
                .andExpect(jsonPath("$.errors").doesNotExist());
        }

        @Test
        void shouldApplyNoCDecisionSuccessfullyWhenRejected() throws Exception {
            ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(CaseDetails.builder()
                .id(CASE_ID)
                .caseTypeId(CASE_TYPE_ID)
                .data(createData(REJECTED))
                .build());

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.Reason").isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.CaseRoleId").isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.NotesReason").isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.ApprovalStatus").isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.RequestTimestamp").isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.ApprovalStatus").isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.ApprovalRejectionTimestamp").isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.OrganisationToAdd.OrganisationID")
                    .isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.OrganisationToAdd.OrganisationName")
                    .isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.OrganisationToRemove.OrganisationID")
                    .isEmpty())
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.OrganisationToRemove.OrganisationName")
                    .isEmpty())
                .andExpect(jsonPath("$.data.OrganisationPolicyField1.Organisation.OrganisationID", is(ORG_1_ID)))
                .andExpect(jsonPath("$.data.OrganisationPolicyField1.Organisation.OrganisationName", is(ORG_1_NAME)))
                .andExpect(jsonPath("$.data.OrganisationPolicyField1.OrgPolicyReference", is(ORG_POLICY_1_REF)))
                .andExpect(jsonPath("$.data.OrganisationPolicyField1.OrgPolicyCaseAssignedRole", is(ORG_POLICY_1_ROLE)))
                .andExpect(jsonPath("$.data.OrganisationPolicyField2.Organisation.OrganisationID", is(ORG_2_ID)))
                .andExpect(jsonPath("$.data.OrganisationPolicyField2.Organisation.OrganisationName", is(ORG_2_NAME)))
                .andExpect(jsonPath("$.data.OrganisationPolicyField2.OrgPolicyReference", is(ORG_POLICY_2_REF)))
                .andExpect(jsonPath("$.data.OrganisationPolicyField2.OrgPolicyCaseAssignedRole", is(ORG_POLICY_2_ROLE)))
                .andExpect(jsonPath("$.data.TextField", is("TextFieldValue")))
                .andExpect(jsonPath("$.errors").doesNotExist());
        }

        @Test
        void shouldNotApplyNoCDecisionWhenNotConsidered() throws Exception {
            ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(CaseDetails.builder()
                .id(CASE_ID)
                .caseTypeId(CASE_TYPE_ID)
                .data(createData(PENDING))
                .build());

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(jsonPath("$.errors[0]", is(NOC_REQUEST_NOT_CONSIDERED)));
        }

        @Test
        void shouldReturnSuccessResponseWithErrorsArrayForHandledExceptions() throws Exception {
            ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(CaseDetails.builder()
                .id(CASE_ID)
                .caseTypeId(CASE_TYPE_ID)
                .build());

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(jsonPath("$.errors[0]", is(NO_DATA_PROVIDED)));
        }

        @Test
        void shouldReturnSuccessResponseWithErrorWhenPrdReturnsNotFoundResponseCode() throws Exception {
            stubFor(WireMock.get(urlEqualTo("/refdata/internal/v1/organisations/UnknownId/users?returnRoles=false"))
                .willReturn(aResponse().withStatus(404)));

            ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(CaseDetails.builder()
                .id(CASE_ID)
                .caseTypeId(CASE_TYPE_ID)
                .data(createData(orgPolicyAsString(null, null, null, null),
                    orgPolicyAsString(ORG_2_ID, ORG_2_NAME, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE),
                    organisationAsString(null, null),
                    organisationAsString("UnknownId", null), APPROVED))
                .build());

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(jsonPath("$.errors[0]", is("Organisation with ID 'UnknownId' can not be found.")));
        }

        @Test
        void shouldReturnSuccessResponseWithErrorWhenPrdReturnsNon404ResponseCode() throws Exception {
            stubFor(WireMock.get(urlEqualTo("/refdata/internal/v1/organisations/OrgId/users?returnRoles=false"))
                .willReturn(aResponse().withStatus(400)));

            ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(CaseDetails.builder()
                .id(CASE_ID)
                .caseTypeId(CASE_TYPE_ID)
                .data(createData(orgPolicyAsString(null, null, null, null),
                    orgPolicyAsString(ORG_2_ID, ORG_2_NAME, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE),
                    organisationAsString(null, null),
                    organisationAsString("OrgId", null), APPROVED))
                .build());

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(jsonPath("$.errors[0]", is("Error encountered while retrieving"
                    + " organisation users for organisation ID 'OrgId': Bad Request")));
        }

        @Test
        void shouldReturnErrorWhenRequestDoesNotContainCaseDetails() throws Exception {
            ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(null);

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.errors.length()", is(1)))
                .andExpect(jsonPath("$.errors[0]", is(CASE_DETAILS_REQUIRED)));
        }

        private String orgPolicyAsString(String organisationId,
                                         String organisationName,
                                         String orgPolicyReference,
                                         String orgPolicyCaseAssignedRole) {
            return String.format("{\"Organisation\":%s,\"OrgPolicyReference\":%s,\"OrgPolicyCaseAssignedRole\":%s}",
                organisationAsString(organisationId, organisationName),
                stringValueAsJson(orgPolicyReference), stringValueAsJson(orgPolicyCaseAssignedRole));
        }

        private String organisationAsString(String organisationId,
                                            String organisationName) {
            return String.format("{\"OrganisationID\":%s,\"OrganisationName\":%s}",
                stringValueAsJson(organisationId), stringValueAsJson(organisationName));
        }

        private String stringValueAsJson(String string) {
            return string == null ? "null" : String.format("\"%s\"", string);
        }

        private String caseRoleIdField(String selectedCode) {
            return String.format("{\n"
                + "\"value\":  {\n"
                + "     \"code\": \"%s\",\n"
                + "     \"label\": \"SomeLabel (Not used)\"\n"
                + "},\n"
                + "\"list_items\" : [\n"
                + "     {\n"
                + "         \"code\": \"[Defendant]\",\n"
                + "         \"label\": \"Defendant\"\n"
                + "     },\n"
                + "     {\n"
                + "         \"code\": \"[Claimant]\",\n"
                + "         \"label\": \"Claimant\"\n"
                + "     }\n"
                + "]\n"
                + "}\n", selectedCode);
        }

        private Map<String, JsonNode> createData(String organisationPolicy1,
                                                 String organisationPolicy2,
                                                 String organisationToAdd,
                                                 String organisationToRemove,
                                                 ApprovalStatus approvalStatus) throws JsonProcessingException {
            return mapper.convertValue(mapper.readTree(String.format("{\n"
                    + "    \"TextField\": \"TextFieldValue\",\n"
                    + "    \"OrganisationPolicyField1\": %s,\n"
                    + "    \"OrganisationPolicyField2\": %s,\n"
                    + "    \"ChangeOrganisationRequestField\": {\n"
                    + "        \"Reason\": null,\n"
                    + "        \"CaseRoleId\": %s,\n"
                    + "        \"NotesReason\": \"a\",\n"
                    + "        \"ApprovalStatus\": %s,\n"
                    + "        \"RequestTimestamp\": null,\n"
                    + "        \"OrganisationToAdd\": %s,\n"
                    + "        \"OrganisationToRemove\": %s,\n"
                    + "        \"ApprovalRejectionTimestamp\": null\n"
                    + "    }\n"
                    + "}", organisationPolicy1, organisationPolicy2, caseRoleIdField("[Claimant]"),
                approvalStatus.getValue(), organisationToAdd, organisationToRemove)), getHashMapTypeReference());
        }

        private Map<String, JsonNode> createData(ApprovalStatus approvalStatus) throws JsonProcessingException {
            return createData(orgPolicyAsString(ORG_1_ID, ORG_1_NAME, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE),
                orgPolicyAsString(ORG_2_ID, ORG_2_NAME, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE),
                organisationAsString(null, null),
                organisationAsString(ORG_2_ID, ORG_2_NAME), approvalStatus);
        }

        private TypeReference<HashMap<String, JsonNode>> getHashMapTypeReference() {
            return new TypeReference<HashMap<String, JsonNode>>() {
            };
        }
    }

    @Nested
    @DisplayName("POST /noc/noc-prepare")
    class PrepareNoticeOfChange extends BaseIT {

        private static final String ENDPOINT_URL = "/noc" + NOC_PREPARE_PATH;

        @Test
        void shouldPrepareNoCEventSuccessfully() throws Exception {

            Map<String, JsonNode> data = new HashMap<>();
            data.put("ChangeOrganisationRequestField", createChangeOrganisationRequest());
            data.put("OrganisationPolicyField", createOrganisationPolicyField());


            CaseDetails caseDetails = CaseDetails.builder()
                .caseTypeId(CASE_TYPE_ID)
                .jurisdiction(JURISDICTION)
                .id(CASE_ID)
                .data(data)
                .state("caseCreated")
                .build();

            AboutToStartCallbackRequest request = new AboutToStartCallbackRequest("prepareOrganisation",
                                                                                  caseDetails, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.CaseRoleId.value.code", is("[Defendant]")))
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.CaseRoleId.value.label", is("Defendant")))
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.CaseRoleId.list_items[0].code",
                    is("[Defendant]")))
                .andExpect(jsonPath("$.data.ChangeOrganisationRequestField.CaseRoleId.list_items[0].label",
                    is("Defendant")))
                .andExpect(jsonPath("$.data.OrganisationPolicyField.Organisation.OrganisationID",
                    is(ORGANISATION_ID)));
        }

        private JsonNode createOrganisationPolicyField() throws JsonProcessingException {
            return mapper.readTree("{\n"
                                       + "            \"Organisation\": {\n"
                                       + "              \"OrganisationID\": \"QUK822N\",\n"
                                       + "              \"OrganisationName\": null\n"
                                       + "            },\n"
                                       + "            \"OrgPolicyReference\": null,\n"
                                       + "            \"OrgPolicyCaseAssignedRole\": \"[Defendant]\"\n"
                                       + "          }");
        }

        private JsonNode createChangeOrganisationRequest() throws JsonProcessingException {
            return mapper.readTree("{\n"
                                       + "        \"Reason\": null,\n"
                                       + "        \"CaseRoleId\": null,\n"
                                       + "        \"NotesReason\": null,\n"
                                       + "        \"ApprovalStatus\": \"2\",\n"
                                       + "        \"RequestTimestamp\": \"2020-11-20T16:17:36.090968\",\n"
                                       + "        \"OrganisationToAdd\": {\n"
                                       + "          \"OrganisationID\": null,\n"
                                       + "          \"OrganisationName\": null\n"
                                       + "        },\n"
                                       + "        \"OrganisationToRemove\": {\n"
                                       + "          \"OrganisationID\": null,\n"
                                       + "          \"OrganisationName\": null\n"
                                       + "        },\n"
                                       + "        \"ApprovalRejectionTimestamp\": null\n"
                                       + "      }");
        }
    }

    @Nested
    @DisplayName("POST /noc/noc-requests")
    class PostNoticeOfChangeRequests extends BaseIT {
        private static final String ENDPOINT_URL = "/noc" + REQUEST_NOTICE_OF_CHANGE_PATH;
        private static final String EVENT_TOKEN = "EVENT_TOKEN";
        private static final String CHANGE_ORGANISATION_REQUEST_FIELD = "changeOrganisationRequestField";
        private static final String ZERO = "0";
        private static final String JURISDICTION = ZERO;
        private static final String USER_ID = ZERO;

        private RequestNoticeOfChangeRequest requestNoticeOfChangeRequest;


        private List<SubmittedChallengeAnswer> submittedChallengeAnswers;

        private CaseDetails caseDetails;

        @BeforeEach
        void setup() {
            submittedChallengeAnswers
                = List.of(new SubmittedChallengeAnswer(QUESTION_ID_1,
                                                       ORGANISATION_ID.toLowerCase(Locale.getDefault())));

            requestNoticeOfChangeRequest = new RequestNoticeOfChangeRequest(CASE_ID, submittedChallengeAnswers);

            CaseUpdateViewEvent caseUpdateViewEvent = CaseUpdateViewEvent.builder()
                .wizardPages(getWizardPages(CHANGE_ORGANISATION_REQUEST_FIELD))
                .eventToken(EVENT_TOKEN)
                .caseFields(getCaseViewFields())
                .build();

            stubGetStartEventTrigger(CASE_ID, NOC, caseUpdateViewEvent);

            Organisation organisationToAdd = Organisation.builder().organisationID(ORGANISATION_ID).build();
            ChangeOrganisationRequest cor = ChangeOrganisationRequest.builder()
                .organisationToAdd(organisationToAdd)
                .createdBy("CreatedByUser")
                .build();

            OrganisationPolicy organisationPolicy = OrganisationPolicy.builder()
                .orgPolicyCaseAssignedRole("Applicant")
                .orgPolicyReference("ApplicantPolicy")
                .organisation(new Organisation(ORGANISATION_ID, ORGANISATION_NAME))
                .build();

            caseFields.clear();
            caseFields.put("ChangeOrgRequest", mapper.convertValue(cor, JsonNode.class));
            caseFields.put("OrganisationPolicy1", mapper.convertValue(organisationPolicy, JsonNode.class));

            caseDetails = CaseDetails.builder().id(CASE_ID).caseTypeId(CASE_TYPE_ID).data(caseFields).build();

            stubGetCaseViaExternalApi(CASE_ID, caseDetails);

            stubSubmitEventForCase(CASE_ID, caseDetails);

            Map<String, JsonNode> data = new HashMap<>();
            CaseDetails caseDetails = CaseDetails.builder().data(data).build();

            StartEventResource startEventResource = StartEventResource.builder()
                .token("token")
                .caseDetails(caseDetails)
                .build();
            stubGetExternalStartEventTrigger(CASE_ID, NOC, startEventResource);

            List<CaseRole> caseRoleList = Arrays.asList(
                CaseRole.builder().id("APPLICANT").name("Applicant").build()
            );
            stubGetCaseRoles(USER_ID, JURISDICTION, CASE_TYPE_ID, caseRoleList);
        }

        @Test
        void shouldSuccessfullyVerifyNoCRequestWithoutAutoApproval() throws Exception {
            this.mockMvc.perform(post(ENDPOINT_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(requestNoticeOfChangeRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status_message", is(REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)))
                .andExpect(jsonPath("$.approval_status", is(PENDING.name())));
        }

        @Test
        void shouldSuccessfullyVerifyNoCRequestWithAutoApproval() throws Exception {

            Organisation org = Organisation.builder().organisationID("InvokingUsersOrg").build();

            OrganisationPolicy orgPolicy = new OrganisationPolicy(org,null,
                                                                  "Applicant",
                                                                  Lists.newArrayList(),
                "createdByUser");

            caseFields.put("OrganisationPolicy", mapper.convertValue(orgPolicy,  JsonNode.class));

            caseDetails = CaseDetails.builder().id(CASE_ID).caseTypeId(CASE_TYPE_ID).data(caseFields).build();

            stubGetCaseViaExternalApi(CASE_ID, caseDetails);

            stubSubmitEventForCase(CASE_ID, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(requestNoticeOfChangeRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status_message", is(REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)))
                .andExpect(jsonPath("$.approval_status", is(APPROVED.name())));
        }
    }

    @Nested
    @DisplayName("POST /noc/check-noc-approval")
    class CheckNoticeOfChangeApproval extends BaseIT {

        private CallbackRequest checkNoticeOfChangeApprovalRequest;
        private CaseDetails caseDetails;
        private ChangeOrganisationRequest changeOrganisationRequest;

        private static final String ENDPOINT_URL = "/noc" + CHECK_NOTICE_OF_CHANGE_APPROVAL_PATH;

        DynamicListElement dynamicListElement = DynamicListElement.builder().code("code").label("label").build();
        DynamicList dynamicList = DynamicList.builder()
            .value(dynamicListElement)
            .listItems(List.of(dynamicListElement))
            .build();

        @BeforeEach
        public void setup() throws JsonProcessingException {

            changeOrganisationRequest = ChangeOrganisationRequest.builder()
                .organisationToAdd(Organisation.builder().organisationID("123").build())
                .organisationToRemove(Organisation.builder().organisationID("789").build())
                .caseRoleId(dynamicList)
                .requestTimestamp(LocalDateTime.now())
                .approvalStatus(APPROVED.name())
                .build();

            caseDetails =  caseDetails(changeOrganisationRequest);

            checkNoticeOfChangeApprovalRequest = new CallbackRequest(NOC, null, caseDetails);

            CaseViewActionableEvent caseViewEvent = new CaseViewActionableEvent();
            caseViewEvent.setId(NOC);
            CaseViewResource caseViewResource = new CaseViewResource();
            caseViewResource.setCaseViewActionableEvents(new CaseViewActionableEvent[]{caseViewEvent});

            Event event = Event.builder()
                .eventId(NOC)
                .summary(NoticeOfChangeApprovalService.APPLY_NOC_DECISION_EVENT)
                .description(NoticeOfChangeApprovalService.APPLY_NOC_DECISION_EVENT)
                .build();

            StartEventResource startEventResource = StartEventResource.builder()
                .eventId(NOC)
                .token("eventToken")
                .caseDetails(caseDetails)
                .build();

            CaseEventCreationPayload caseEventCreationPayload = CaseEventCreationPayload.builder()
                .token(startEventResource.getToken())
                .event(event)
                .data(caseDetails.getData())
                .build();

            CaseDetails caseDetails = CaseDetails.builder().build();

            stubGetCaseInternalAsApprover(CASE_ID, caseViewResource);
            stubGetExternalStartEventTriggerAsApprover(CASE_ID, NOC, startEventResource);
            stubSubmitEventForCase(CASE_ID, caseEventCreationPayload, caseDetails);
        }

        @Test
        void shouldSuccessfullyCheckNoCApprovalWithAutoApproval() throws Exception {
            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(checkNoticeOfChangeApprovalRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmation_header", is(CHECK_NOC_APPROVAL_DECISION_APPLIED_MESSAGE)))
                .andExpect(jsonPath("$.confirmation_body", is(CHECK_NOC_APPROVAL_DECISION_APPLIED_MESSAGE)));
        }

        @Test
        void shouldSuccessfullyCheckNoCApprovalWithoutAutoApproval() throws Exception {
            changeOrganisationRequest = ChangeOrganisationRequest.builder()
                .organisationToAdd(Organisation.builder().organisationID("123").build())
                .organisationToRemove(Organisation.builder().organisationID("789").build())
                .caseRoleId(dynamicList)
                .requestTimestamp(LocalDateTime.now())
                .approvalStatus(PENDING.name())
                .build();

            caseDetails =  caseDetails(changeOrganisationRequest);
            checkNoticeOfChangeApprovalRequest = new CallbackRequest(NOC, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(checkNoticeOfChangeApprovalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmation_header", is(CHECK_NOC_APPROVAL_DECISION_NOT_APPLIED_MESSAGE)))
                .andExpect(jsonPath("$.confirmation_body", is(CHECK_NOC_APPROVAL_DECISION_NOT_APPLIED_MESSAGE)));
        }

        @Test
        void shouldReturnAnErrorIfRequestDoesNotContainChangeOrgRequest() throws Exception {
            caseDetails = defaultCaseDetails().build();

            checkNoticeOfChangeApprovalRequest = new CallbackRequest(NOC, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(checkNoticeOfChangeApprovalRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.message", is(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID)));
        }

        @Test
        void shouldReturnAnErrorIfChangeOrganisationRequestIsInvalid() throws Exception {
            changeOrganisationRequest.setApprovalStatus(null);
            caseDetails =  caseDetails(changeOrganisationRequest);
            checkNoticeOfChangeApprovalRequest = new CallbackRequest(NOC, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(checkNoticeOfChangeApprovalRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.message", is(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID)));
        }
    }

    @Nested
    @DisplayName("POST /noc/set-organisation-to-remove")
    class SetOrganisationToRemove extends BaseIT {

        private CallbackRequest noticeOfChangeRequest;
        private CaseDetails caseDetails;
        private ChangeOrganisationRequest changeOrganisationRequest;

        private static final String ENDPOINT_URL = "/noc" + SET_ORGANISATION_TO_REMOVE_PATH;

        DynamicListElement dynamicListElement = DynamicListElement.builder().code("Role1").label("label").build();
        DynamicList dynamicList = DynamicList.builder()
            .value(dynamicListElement)
            .listItems(List.of(dynamicListElement))
            .build();

        @BeforeEach
        public void setup() {
            changeOrganisationRequest = ChangeOrganisationRequest.builder()
                .organisationToAdd(Organisation.builder().organisationID("123").build())
                .organisationToRemove(Organisation.builder().organisationID(null).build())
                .caseRoleId(dynamicList)
                .requestTimestamp(LocalDateTime.now())
                .approvalStatus("APPROVED")
                .build();

            Organisation organisation = Organisation.builder()
                .organisationID("Org1")
                .build();

            OrganisationPolicy organisationPolicy = OrganisationPolicy.builder()
                .organisation(organisation)
                .orgPolicyReference("PolicyRef")
                .orgPolicyCaseAssignedRole("Role1")
                .build();

            caseDetails = caseDetails(changeOrganisationRequest, organisationPolicy);
            noticeOfChangeRequest = new CallbackRequest(NOC, null, caseDetails);
        }

        @Test
        void shouldSuccessfullySetOrganisationToRemove() throws Exception {
            this.mockMvc.perform(post(ENDPOINT_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(noticeOfChangeRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.data.changeOrganisationRequestField.OrganisationToRemove.OrganisationID",
                    is("Org1")));
        }

        @Test
        void shouldReturnAnErrorIfRequestDoesNotContainChangeOrgRequest() throws Exception {
            caseDetails = defaultCaseDetails().data(Map.of()).build();
            noticeOfChangeRequest = new CallbackRequest(NOC, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(noticeOfChangeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.message", is(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID)));
        }

        @Test
        void shouldReturnAnErrorIfChangeOrganisationRequestIsInvalid() throws Exception {
            changeOrganisationRequest.setApprovalStatus(null);
            caseDetails = caseDetails(changeOrganisationRequest);
            noticeOfChangeRequest = new CallbackRequest(NOC, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(noticeOfChangeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.message", is(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID)));
        }

        @Test
        void shouldReturnAnErrorIfOrganisationToRemoveIsInvalid() throws Exception {
            changeOrganisationRequest.setOrganisationToRemove(Organisation.builder().organisationID("Org2").build());
            caseDetails = caseDetails(changeOrganisationRequest);
            noticeOfChangeRequest = new CallbackRequest(NOC, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(noticeOfChangeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.message", is(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID)));
        }

        @Test
        void shouldReturnAnErrorIfNoMatchingOrganisationPolicies() throws Exception {
            caseDetails = caseDetails(changeOrganisationRequest);
            noticeOfChangeRequest = new CallbackRequest(NOC, null, caseDetails);

            this.mockMvc.perform(post(ENDPOINT_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(noticeOfChangeRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.message", is(INVALID_CASE_ROLE_FIELD)));
        }
    }
}
