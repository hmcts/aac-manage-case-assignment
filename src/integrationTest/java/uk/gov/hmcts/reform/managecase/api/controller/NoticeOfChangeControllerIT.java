package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeRequest;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseResource;
import uk.gov.hmcts.reform.managecase.client.datastore.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseUpdateViewEvent;
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
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.domain.SubmittedChallengeAnswer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.GET_NOC_QUESTIONS;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.VERIFY_NOC_ANSWERS;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.VERIFY_NOC_ANSWERS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.PREDEFINED_COMPLEX_ORGANISATION_POLICY;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseInternal;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseInternalES;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseViaExternalApi;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetChallengeQuestions;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetStartEventTrigger;

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
    private static final String NOC = "NOC";

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws JsonProcessingException {
        CaseViewActionableEvent caseViewActionableEvent = new CaseViewActionableEvent();
        caseViewActionableEvent.setId(NOC);

        CaseViewResource caseViewResource = new CaseViewResource();
        caseViewResource.setCaseViewActionableEvents(new CaseViewActionableEvent[]{caseViewActionableEvent});
        caseViewResource.setReference(CASE_ID);

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
        searchResultViewHeader.setCaseFieldTypeDefinition(fieldTypeDefinition);
        searchResultViewHeader.setCaseFieldId("changeOrg");
        SearchResultViewHeaderGroup correctHeader = new SearchResultViewHeaderGroup(
            new HeaderGroupMetadata(JURISDICTION, CASE_TYPE_ID),
            Arrays.asList(searchResultViewHeader),
            Arrays.asList("111", "222")
        );
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

        FieldType fieldType = new FieldType();
        fieldType.setId("Number");
        fieldType.setType("Number");
        fieldType.setMin(null);
        fieldType.setMax(null);
        ChallengeQuestion challengeQuestion = new ChallengeQuestion(CASE_TYPE_ID, 1,
            "questionText",
            fieldType,
            null,
            "NoC",
            ANSWER_FIELD_APPLICANT,
            QUESTION_ID_1,
            null);
        ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(
            Arrays.asList(challengeQuestion));
        stubGetChallengeQuestions(CASE_TYPE_ID, "NoCChallenge", challengeQuestionsResult);

        stubGetStartEventTrigger(CASE_ID, NOC, CaseUpdateViewEvent.builder().build());

        CaseResource caseResource = CaseResource.builder().data(caseFields).build();
        stubGetCaseViaExternalApi(CASE_ID, caseResource);
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
            VerifyNoCAnswersRequest request = new VerifyNoCAnswersRequest(CASE_ID, Collections.singletonList(answer));

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
            VerifyNoCAnswersRequest request = new VerifyNoCAnswersRequest(CASE_ID, Collections.singletonList(answer));

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
            VerifyNoCAnswersRequest request = new VerifyNoCAnswersRequest(CASE_ID, Collections.singletonList(answer));

            this.mockMvc.perform(post(ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.message", is("No answer has been provided for question ID 'QuestionId1'")));
        }
    }

    @Nested
    @DisplayName("POST /noc/noc-requests")
    class PostNoticeOfChangeRequests extends BaseTest {
        private static final String ENDPOINT_URL = "/noc" + REQUEST_NOTICE_OF_CHANGE_PATH;

        @Autowired
        private MockMvc mockMvc;

        private RequestNoticeOfChangeRequest requestNoticeOfChangeRequest;

        List<SubmittedChallengeAnswer> submittedChallengeAnswers;

        @BeforeEach
        public void setup() {
            submittedChallengeAnswers
                = List.of(new SubmittedChallengeAnswer(QUESTION_ID_1,
                                                       ORGANISATION_ID.toLowerCase(Locale.getDefault())));

            requestNoticeOfChangeRequest = new RequestNoticeOfChangeRequest(CASE_ID, submittedChallengeAnswers);
        }

        @Test
        void shouldSuccessfullyVerifyNoCRequestWithoutAutoApproval() throws Exception {
            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(requestNoticeOfChangeRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status_message", is(REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)))
                .andExpect(jsonPath("$.approval_status", is("PENDING")));
        }

        @Test
        void shouldSuccessfullyVerifyNoCRequestWithAutoApproval() throws Exception {
            // these values come from PRD - in this case the wiremock config file `prd_users.json`
            Organisation org = new Organisation("InvokingUsersOrg", "");
            OrganisationPolicy orgPolicy = new OrganisationPolicy(org, null, "Applicant");

            caseFields.put("OrganisationPolicy", mapper.convertValue(orgPolicy,  JsonNode.class));
            caseFields.put("ChangeOrgRequest", mapper.convertValue(ChangeOrganisationRequest.builder().build(),
                                                                   JsonNode.class));
            CaseResource caseResource = CaseResource.builder().data(caseFields).build();

            stubGetCaseViaExternalApi(CASE_ID, caseResource);

            this.mockMvc.perform(post(ENDPOINT_URL)
                                     .contentType(MediaType.APPLICATION_JSON)
                                     .content(mapper.writeValueAsString(requestNoticeOfChangeRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.status_message", is(REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)))
                .andExpect(jsonPath("$.approval_status", is("APPROVED")));
        }
    }
}
