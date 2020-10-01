package uk.gov.hmcts.reform.managecase.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.client.datastore.model.AuditEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewEvent;
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
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.PREDEFINED_COMPLEX_ORGANISATION_POLICY;

@SuppressWarnings({"PMD.MethodNamingConventions", "PMD.JUnitAssertionsShouldIncludeMessage", "PMD.ExcessiveImports"})
class NoticeOfChangeServiceTest {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String CASE_ID = "1567934206391385";
    private static final String JURISDICTION = "Jurisdiction";
    private static final String CASE_TYPE = "CaseTypeA";

    @InjectMocks
    private NoticeOfChangeService service;

    @Mock
    private DataStoreRepository dataStoreRepository;
    @Mock
    private DefinitionStoreRepository definitionStoreRepository;
    @Mock
    private IdamRepository idamRepository;
    @Mock
    private ChallengeQuestionService challengeQuestionService;
    @Mock
    private PrdRepository prdRepository;
    @Mock
    private RequestContext context;

    private List<SearchResultViewItem> viewItems = new ArrayList<>();
    private Map<String, JsonNode> caseFields = new HashMap<>();
    private static final String DATE_FIELD = "DateField";
    private static final String DATETIME_FIELD = "DateTimeField";
    private static final String TEXT_FIELD = "TextField";

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Nested
    @DisplayName("Get NoC Questions")
    class AssignCaseAccess {

        private NoticeOfChangeService noticeOfChangeService;

        @BeforeEach
        void setUp()throws JsonProcessingException {
            noticeOfChangeService = new NoticeOfChangeService(dataStoreRepository, idamRepository,
                definitionStoreRepository, challengeQuestionService, prdRepository);

            //internal/cases/caseId
            AuditEvent event = new AuditEvent();
            event.setEventName("NOC");
            CaseViewEvent caseViewEvent = CaseViewEvent.createFrom(event);
            CaseViewResource caseViewResource = new CaseViewResource();
            CaseViewEvent[] caseViewEventArray = {caseViewEvent};
            caseViewResource.setCaseViewEvents(caseViewEventArray);
            CaseViewType caseViewType = new CaseViewType();
            caseViewType.setName(CASE_TYPE_ID);
            CaseViewJurisdiction caseViewJurisdiction = new CaseViewJurisdiction();
            caseViewJurisdiction.setId(JURISDICTION);
            caseViewType.setJurisdiction(caseViewJurisdiction);
            caseViewResource.setCaseType(caseViewType);
            given(dataStoreRepository.findCaseByCaseId(CASE_ID))
                .willReturn(caseViewResource);

            // Internal ES query
            SearchResultViewHeader searchResultViewHeader = new SearchResultViewHeader();
            FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
            fieldTypeDefinition.setType(PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST);
            searchResultViewHeader.setCaseFieldTypeDefinition(fieldTypeDefinition);
            searchResultViewHeader.setCaseFieldId("changeOrg");
            caseFields.put(DATE_FIELD, new TextNode("2020-10-01"));
            caseFields.put(DATETIME_FIELD, new TextNode("1985-12-30"));
            caseFields.put(TEXT_FIELD, new TextNode("Text Value"));
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
            SearchResultViewHeaderGroup correctHeader = new SearchResultViewHeaderGroup(
                new HeaderGroupMetadata(JURISDICTION, CASE_TYPE),
                Arrays.asList(searchResultViewHeader), Arrays.asList("111", "222")
            );
            List<SearchResultViewHeaderGroup> headers = new ArrayList<>();
            headers.add(correctHeader);
            List<SearchResultViewItem> cases = new ArrayList<>();
            cases.add(item);
            Long total = 3L;
            CaseSearchResultView caseSearchResultView = new CaseSearchResultView(headers, cases, total);
            CaseSearchResultViewResource resource = new CaseSearchResultViewResource(caseSearchResultView);
            given(dataStoreRepository.findCaseBy(CASE_TYPE_ID, null, CASE_ID)).willReturn(resource);

            //Challenge Questions
            ChallengeQuestion challengeQuestion = new ChallengeQuestion();
            challengeQuestion.setCaseTypeId(CASE_TYPE_ID);
            challengeQuestion.setOrder(1);
            challengeQuestion.setQuestionText("QuestionText1");
            FieldType fieldType = new FieldType();
            fieldType.setId("Number");
            fieldType.setType("Number");
            fieldType.setMin(null);
            fieldType.setMax(null);
            challengeQuestion.setAnswerFieldType(fieldType);
            challengeQuestion.setChallengeQuestionId("NoC");
            challengeQuestion.setAnswerField("${applicant.individual.fullname}|${applicant.company.name}|"
                                                 + "${applicant.soletrader.name}:Applicant,"
                                                 + "${respondent.individual.fullname}|${respondent.company.name}"
                                                 + "{|${respondent.soletrader.name}:Applicant");
            challengeQuestion.setQuestionId("QuestionId1");
            ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(
                Arrays.asList(challengeQuestion));
            given(definitionStoreRepository.challengeQuestions(CASE_TYPE_ID, CASE_ID))
                .willReturn(challengeQuestionsResult);

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/url");
            RequestContext context = new RequestContext();
            context.setRequest(request);
            RequestContext.testSetCurrentContext(context);

            UserInfo userInfo = new UserInfo("","","", "", "", Arrays.asList("pui-caa"));
            given(idamRepository.getUserInfo(context.getRequest().getAuthType())).willReturn(userInfo);
        }

        @Test
        @DisplayName("NoC questions successfully returned for a Solicitor user")
        void shouldReturnQuestionsForSolicitor() {
            ChallengeQuestionsResult challengeQuestionsResult = service.getChallengeQuestions(CASE_ID);

            assertThat(challengeQuestionsResult).isNotNull();
            assertThat(challengeQuestionsResult.getQuestions().get(0).getCaseTypeId()).isEqualTo(CASE_TYPE_ID);
            assertThat(challengeQuestionsResult.getQuestions().get(0).getOrder()).isEqualTo(1);
            assertThat(challengeQuestionsResult.getQuestions().get(0).getQuestionText()).isEqualTo("QuestionText1");
            assertThat(challengeQuestionsResult.getQuestions().get(0).getChallengeQuestionId()).isEqualTo("NoC");
            assertThat(challengeQuestionsResult.getQuestions().get(0).getAnswers().size()).isEqualTo(0);
        }

        @Test
        @DisplayName("must return an error response when a NoC request is currently pending on a case")
        void shouldThrowErrorNoCRequestPending() throws JsonProcessingException {

            SearchResultViewHeader searchResultViewHeader = new SearchResultViewHeader();
            FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
            fieldTypeDefinition.setType(PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST);
            searchResultViewHeader.setCaseFieldTypeDefinition(fieldTypeDefinition);
            searchResultViewHeader.setCaseFieldId("changeOrg");

            caseFields.put(DATE_FIELD, new TextNode("2020-10-01"));
            caseFields.put(DATETIME_FIELD, new TextNode("1985-12-30"));
            caseFields.put(TEXT_FIELD, new TextNode("Text Value"));
            ObjectMapper mapper = new ObjectMapper();
            JsonNode actualObj = mapper.readValue("   {\n"
                                                      + "                \"changeOrg\":\n"
                                                      + "                {\n"
                                                      + "                    \"CaseRoleId\": \"role\"\n"
                                                      + "                }\n"
                                                      + "            }", JsonNode.class);
            caseFields.put("changeOrg", actualObj);
            SearchResultViewItem item = new SearchResultViewItem("CaseId", caseFields, caseFields);
            viewItems.add(item);
            SearchResultViewHeaderGroup correctHeader = new SearchResultViewHeaderGroup(
                new HeaderGroupMetadata(JURISDICTION, CASE_TYPE),
                Arrays.asList(searchResultViewHeader), Arrays.asList("111", "222")
            );
            List<SearchResultViewHeaderGroup> headers = new ArrayList<>();
            headers.add(correctHeader);
            List<SearchResultViewItem> cases = new ArrayList<>();
            cases.add(item);
            Long total = 3L;

            CaseSearchResultView caseSearchResultView = new CaseSearchResultView(headers, cases, total);

            CaseSearchResultViewResource resource = new CaseSearchResultViewResource(caseSearchResultView);

            given(dataStoreRepository.findCaseBy(CASE_TYPE_ID, null, CASE_ID)).willReturn(resource);
            assertThatThrownBy(() -> service.getChallengeQuestions(CASE_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("on going NoC request in progress");
        }

        @Test
        @DisplayName("Must return an error if there is not an Organisation Policy field")
        void shouldThrowErrorMissingOrgPolicy() throws JsonProcessingException {
            ChallengeQuestion challengeQuestion = new ChallengeQuestion();
            challengeQuestion.setCaseTypeId(CASE_TYPE_ID);
            challengeQuestion.setOrder(1);
            challengeQuestion.setQuestionText("QuestionText1");
            FieldType fieldType = new FieldType();
            fieldType.setId("Number");
            fieldType.setType("Number");
            fieldType.setMin(null);
            fieldType.setMax(null);
            challengeQuestion.setAnswerFieldType(fieldType);
            challengeQuestion.setChallengeQuestionId("NoC");
            challengeQuestion.setAnswerField("${applicant.individual.fullname}|${applicant.company.name}|"
                                                 + "${applicant.soletrader.name}:Applicant,"
                                                 + "${respondent.individual.fullname}|${respondent.company.name}"
                                                 + "{|${respondent.soletrader.name}:Respondent");
            challengeQuestion.setQuestionId("QuestionId1");
            ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(
                Arrays.asList(challengeQuestion));
            given(definitionStoreRepository.challengeQuestions(CASE_TYPE_ID, CASE_ID))
                .willReturn(challengeQuestionsResult);

            assertThatThrownBy(() -> service.getChallengeQuestions(CASE_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("No Org Policy with that role");
        }

        @Test
        @DisplayName("Must return an error if there is not an Organisation Policy field containing a "
            + "case role for each set of answers")
        void shouldThrowErrorMissingRoleInOrgPolicy() throws JsonProcessingException {
            ChallengeQuestion challengeQuestion = new ChallengeQuestion();
            challengeQuestion.setCaseTypeId(CASE_TYPE_ID);
            challengeQuestion.setOrder(1);
            challengeQuestion.setQuestionText("QuestionText1");
            FieldType fieldType = new FieldType();
            fieldType.setId("Number");
            fieldType.setType("Number");
            fieldType.setMin(null);
            fieldType.setMax(null);
            challengeQuestion.setAnswerFieldType(fieldType);
            challengeQuestion.setChallengeQuestionId("NoC");
            challengeQuestion.setAnswerField("${applicant.individual.fullname}|${applicant.company.name}|"
                                                 + "${applicant.soletrader.name}:Applicant,"
                                                 + "${respondent.individual.fullname}|${respondent.company.name}"
                                                 + "{|${respondent.soletrader.name}:Respondent");
            challengeQuestion.setQuestionId("QuestionId1");
            ChallengeQuestionsResult challengeQuestionsResult = new ChallengeQuestionsResult(
                Arrays.asList(challengeQuestion));
            given(definitionStoreRepository
                      .challengeQuestions(CASE_TYPE_ID, CASE_ID))
                .willReturn(challengeQuestionsResult);

            assertThatThrownBy(() -> service.getChallengeQuestions(CASE_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("No Org Policy with that role");
        }

        @Test
        @DisplayName("Must return an error when no NOC Request event is available on the case")
        void shouldThrowErrorNoEventAvailable() {
            AuditEvent event = new AuditEvent();
            event.setEventName("NoC Event");
            CaseViewEvent caseViewEvent = CaseViewEvent.createFrom(event);
            CaseViewResource caseViewResource = new CaseViewResource();
            CaseViewEvent[] caseViewEventArray = {caseViewEvent};
            caseViewResource.setCaseViewEvents(caseViewEventArray);
            given(dataStoreRepository.findCaseByCaseId(CASE_ID))
                .willReturn(caseViewResource);

            assertThatThrownBy(() -> service.getChallengeQuestions(CASE_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("no NoC events available for this case type");
        }

        @Test
        @DisplayName(" must return an error response when the solicitor does not have access to the "
            + "jurisdiction of the case")
        void shouldThrowErrorInsufficientPrivilegesForSolicitor() {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/url");
            RequestContext context = new RequestContext();
            context.setRequest(request);
            RequestContext.testSetCurrentContext(context);
            UserInfo userInfo = new UserInfo("","","", "", "", Arrays.asList("caseworker-JurisdictionA-solicitor"));
            given(idamRepository.getUserInfo(context.getRequest().getAuthType())).willReturn(userInfo);

            assertThatThrownBy(() -> service.getChallengeQuestions(CASE_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("insufficient privileges");
        }

    }
}
