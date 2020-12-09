package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.managecase.BaseTest;
import uk.gov.hmcts.reform.managecase.api.payload.ApplyNoCDecisionRequest;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.ApprovalStatus;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewActionableEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewJurisdiction;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewType;
import uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultView;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.HeaderGroupMetadata;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewHeader;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewHeaderGroup;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.reform.managecase.domain.SubmittedChallengeAnswer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.user;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.usersByOrganisation;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.APPLY_NOC_DECISION;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.GET_NOC_QUESTIONS;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.VERIFY_NOC_ANSWERS;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.VERIFY_NOC_ANSWERS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_DETAILS_REQUIRED;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_REQUEST_NOT_CONSIDERED;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_DATA_PROVIDED;
import static uk.gov.hmcts.reform.managecase.client.datastore.ApprovalStatus.APPROVED;
import static uk.gov.hmcts.reform.managecase.client.datastore.ApprovalStatus.NOT_CONSIDERED;
import static uk.gov.hmcts.reform.managecase.client.datastore.ApprovalStatus.REJECTED;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.PREDEFINED_COMPLEX_ORGANISATION_POLICY;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseAssignments;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseInternal;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseInternalES;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetChallengeQuestions;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetUsersByOrganisationInternal;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubUnassignCase;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.MethodNamingConventions",
    "PMD.AvoidDuplicateLiterals", "PMD.ExcessiveImports", "PMD.TooManyMethods", "PMD.UseConcurrentHashMap",
    "PMD.DataflowAnomalyAnalysis", "squid:S100", "squid:S1192"})
public class NoticeOfChangeControllerIT {

    private static final String CASE_ID = "1588234985453946";
    private static final String CASE_TYPE_ID = "caseType";
    private static final String JURISDICTION = "Jurisdiction";

    private static final String RAW_QUERY = "{\"query\":{\"bool\":{\"filter\":{\"term\":{\"reference\":%s}}}}}";
    private static final String ES_QUERY = String.format(RAW_QUERY, CASE_ID);
    private static final String QUESTION_ID_1 = "QuestionId1";
    public static final String ORGANISATION_ID = "QUK822N";
    private final Map<String, JsonNode> caseFields = new HashMap<>();
    private final List<SearchResultViewItem> viewItems = new ArrayList<>();
    private static final String ANSWER_FIELD_APPLICANT = "${applicant.individual.fullname}|${applicant.company.name}|"
        + "${applicant.soletrader.name}|${OrganisationPolicy.OrganisationPolicy1"
        + ".Organisation.OrganisationID}:Applicant";

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws JsonProcessingException {
        CaseViewActionableEvent caseViewActionableEvent = new CaseViewActionableEvent();
        caseViewActionableEvent.setId("NOC");
        CaseViewResource caseViewResource = new CaseViewResource();
        caseViewResource.setCaseViewActionableEvents(new CaseViewActionableEvent[] {caseViewActionableEvent});
        CaseViewType caseViewType = new CaseViewType();
        caseViewType.setId(CASE_TYPE_ID);
        CaseViewJurisdiction caseViewJurisdiction = new CaseViewJurisdiction();
        caseViewJurisdiction.setId(JURISDICTION);
        caseViewType.setJurisdiction(caseViewJurisdiction);
        caseViewResource.setCaseType(caseViewType);
        stubGetCaseInternal(CASE_ID, caseViewResource);


        SearchResultViewHeader searchResultViewHeader = new SearchResultViewHeader();
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setType(PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST);
        fieldTypeDefinition.setId(PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST);
        searchResultViewHeader.setCaseFieldTypeDefinition(fieldTypeDefinition);
        searchResultViewHeader.setCaseFieldId("changeOrg");
        SearchResultViewHeaderGroup correctHeader = new SearchResultViewHeaderGroup(
            new HeaderGroupMetadata(JURISDICTION, CASE_TYPE_ID),
            Arrays.asList(searchResultViewHeader),
            Arrays.asList("111", "222")
        );
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readValue("{\n"
                                                    + "  \"OrganisationPolicy1\": {\n"
                                                    + "    \"OrgPolicyCaseAssignedRole\": \"Applicant\",\n"
                                                    + "    \"OrgPolicyReference\": \"Reference\",\n"
                                                    + "    \"Organisation\": {\n"
                                                    + "      \"OrganisationID\": \"QUK822N\",\n"
                                                    + "      \"OrganisationName\": \"CCD Solicitors Limited\"\n"
                                                    + "    }\n"
                                                    + "  }\n"
                                                    + "}", JsonNode.class);

        caseFields.put(PREDEFINED_COMPLEX_ORGANISATION_POLICY, actualObj);
        SearchResultViewItem item = new SearchResultViewItem("CaseId", caseFields, caseFields);
        viewItems.add(item);
        List<SearchResultViewHeaderGroup> headers = new ArrayList<>();
        headers.add(correctHeader);
        List<SearchResultViewItem> cases = new ArrayList<>();
        cases.add(item);
        Long total = 3L;
        CaseSearchResultView caseSearchResultView = new CaseSearchResultView(headers, cases, total);
        CaseSearchResultViewResource resource = new CaseSearchResultViewResource(caseSearchResultView);
        stubGetCaseInternalES(CASE_TYPE_ID, ES_QUERY, resource);

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
    }

    @Nested
    @DisplayName("GET /noc/noc-questions")
    class GetNoticeOfChangeQuestions extends BaseTest {

        @Autowired
        private MockMvc mockMvc;

        @DisplayName("Successfully return NoC questions for case id")
        @Test
        void shouldGetNoCQuestions_forAValidRequest() throws Exception {
            this.mockMvc.perform(get("/noc" + GET_NOC_QUESTIONS)
                                     .queryParam("case_id", CASE_ID))
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

            this.mockMvc.perform(get("/noc" + GET_NOC_QUESTIONS)
                                     .queryParam("case_id", ""))
                .andExpect(status().isBadRequest());
        }

    }

    @Nested
    @DisplayName("POST /noc/verify-noc-answers")
    class VerifyNoticeOfChangeQuestions extends BaseTest {

        private static final String ENDPOINT_URL = "/noc" + VERIFY_NOC_ANSWERS;

        @Autowired
        private MockMvc mockMvc;

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
    class ApplyNoticeOfChangeDecision extends BaseTest {

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

        @Autowired
        private MockMvc mockMvc;

        @BeforeEach
        void setUp() throws JsonProcessingException {
            CaseUserRole caseUserRole = CaseUserRole.builder()
                .caseId(CASE_ID)
                .userId(USER_ID_2)
                .caseRole(ORG_POLICY_2_ROLE)
                .build();
            stubGetCaseAssignments(singletonList(CASE_ID), singletonList(caseUserRole));
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
                .data(createData(NOT_CONSIDERED))
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
                approvalStatus.getCode(), organisationToAdd, organisationToRemove)), getHashMapTypeReference());
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
}
