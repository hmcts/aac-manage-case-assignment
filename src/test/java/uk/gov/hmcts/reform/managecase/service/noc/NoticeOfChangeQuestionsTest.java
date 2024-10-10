package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseCouldNotBeFoundException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCException;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewActionableEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewJurisdiction;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewType;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeAnswer;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.DynamicList;
import uk.gov.hmcts.reform.managecase.domain.DynamicListElement;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.organisationPolicy;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.organisationPolicyJsonNode;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_NOT_FOUND;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CHANGE_REQUEST;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.INSUFFICIENT_PRIVILEGE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.MULTIPLE_NOC_REQUEST_EVENTS;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_EVENT_NOT_AVAILABLE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_REQUEST_ONGOING;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_ORG_POLICY_WITH_ROLE;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.PENDING;

@SuppressWarnings({"PMD.UseConcurrentHashMap",
    "PMD.AvoidDuplicateLiterals",
    "PMD.ExcessiveImports",
    "PMD.TooManyMethods",
    "PMD.DataflowAnomalyAnalysis"})
class NoticeOfChangeQuestionsTest {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String CASE_ID = "1567934206391385";
    private static final String JURISDICTION = "Jurisdiction";
    private static final String CASE_TYPE = "CaseTypeA";
    private static final String QUESTION_TEXT = "QuestionText1";
    private static final String FIELD_ID = "Number";
    private static final String CHANGE_ORG = "changeOrg";
    private static final String CHALLENGE_QUESTION = "NoC";
    private static final String ANSWER_FIELD = "${applicant.individual.fullname}|${applicant.company.name}|"
        + "${applicant.soletrader.name}:Applicant,${respondent.individual.fullname}|${respondent.company.name}"
        + "|${respondent.soletrader.name}:Respondent";
    private static final String ANSWER_FIELD_APPLICANT = "${applicant.individual.fullname}|${applicant.company.name}|"
        + "${applicant.soletrader.name}:Applicant";
    private static final String ANSWER_FIELD_RESPONDENT = "${applicant.individual.fullname}|${applicant.company.name}|"
        + "${applicant.soletrader.name}:Respondent";

    private static final String ORG_POLICY_ROLE = "Applicant";
    private static final String ORGANIZATION_ID = "QUK822N";

    @InjectMocks
    private NoticeOfChangeQuestions service;

    @Mock
    private DataStoreRepository dataStoreRepository;
    @Mock
    private DefinitionStoreRepository definitionStoreRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private JacksonUtils jacksonUtils;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Nested
    @DisplayName("Get NoC Questions")
    class AssignCaseAccess {

        @BeforeEach
        void setUp() {
            //internal/cases/caseId
            CaseViewActionableEvent caseViewActionableEvent = new CaseViewActionableEvent();
            caseViewActionableEvent.setId("NOC");
            CaseViewResource caseViewResource = new CaseViewResource();
            caseViewResource.setCaseViewActionableEvents(new CaseViewActionableEvent[]{caseViewActionableEvent});
            CaseViewType caseViewType = new CaseViewType();
            caseViewType.setName(CASE_TYPE_ID);
            caseViewType.setId(CASE_TYPE_ID);
            CaseViewJurisdiction caseViewJurisdiction = new CaseViewJurisdiction();
            caseViewJurisdiction.setId(JURISDICTION);
            caseViewType.setJurisdiction(caseViewJurisdiction);
            caseViewResource.setCaseType(caseViewType);
            given(dataStoreRepository.findCaseByCaseId(CASE_ID))
                .willReturn(caseViewResource);

            // external case by id
            Organisation organisationToAdd = Organisation.builder().organisationID(ORGANIZATION_ID).build();
            ChangeOrganisationRequest cor = ChangeOrganisationRequest.builder()
                .organisationToAdd(organisationToAdd)
                .build();

            Map<String, JsonNode> data = new HashMap<>();
            data.put(
                "CouldBeAnyChangeOrganisationRequestField",
                new ObjectMapper().convertValue(cor, JsonNode.class)
            );

            data.put("OrganisationPolicy1", organisationPolicyJsonNode(ORGANIZATION_ID, ORG_POLICY_ROLE));

            CaseDetails caseDetails = CaseDetails.builder().id(CASE_ID).caseTypeId(CASE_TYPE_ID).data(data).build();

            given(dataStoreRepository.findCaseByCaseIdAsSystemUserUsingExternalApi(CASE_ID)).willReturn(caseDetails);
            given(jacksonUtils.convertValue(any(JsonNode.class), eq(OrganisationPolicy.class)))
                .willReturn(organisationPolicy(ORGANIZATION_ID, ORG_POLICY_ROLE));

            //Challenge Questions
            FieldType fieldType = FieldType.builder()
                .max(null)
                .min(null)
                .id(FIELD_ID)
                .type(FIELD_ID)
                .build();
            ChallengeAnswer challengeAnswer = new ChallengeAnswer(ANSWER_FIELD_APPLICANT);
            ChallengeQuestion challengeQuestion = ChallengeQuestion.builder()
                .caseTypeId(CASE_TYPE_ID)
                .challengeQuestionId("NoC")
                .questionText("QuestionText1")
                .answerFieldType(fieldType)
                .answerField(ANSWER_FIELD)
                .answers(Arrays.asList(challengeAnswer))
                .questionId("NoC")
                .order(1).build();
            ChallengeQuestionsResult challengeQuestionsResult =
                new ChallengeQuestionsResult(Arrays.asList(challengeQuestion));
            given(definitionStoreRepository.challengeQuestions(CASE_TYPE_ID, "NoCChallenge"))
                .willReturn(challengeQuestionsResult);

            UserInfo userInfo = new UserInfo("", "", "", "", "",
                                             Arrays.asList("pui-caa")
            );
            given(securityUtils.getUserInfo()).willReturn(userInfo);
        }

        @Test
        @DisplayName("NoC questions successfully returned for a Solicitor user")
        void shouldReturnQuestionsForSolicitor() {
            UserInfo userInfo = new UserInfo("", "", "", "", "",
                                             Arrays.asList("caseworker-test", "caseworker-Jurisdiction-solicitor")
            );
            given(securityUtils.getUserInfo()).willReturn(userInfo);
            given(securityUtils.hasSolicitorAndJurisdictionRoles(anyList(), any())).willReturn(true);
            ChallengeQuestionsResult challengeQuestionsResult = service.getChallengeQuestions(CASE_ID);

            assertThat(challengeQuestionsResult).isNotNull();
            assertThat(challengeQuestionsResult.getQuestions().get(0).getCaseTypeId()).isEqualTo(CASE_TYPE_ID);
            assertThat(challengeQuestionsResult.getQuestions().get(0).getOrder()).isEqualTo(1);
            assertThat(challengeQuestionsResult.getQuestions().get(0).getQuestionText()).isEqualTo(QUESTION_TEXT);
            assertThat(challengeQuestionsResult.getQuestions().get(0).getChallengeQuestionId())
                .isEqualTo(CHALLENGE_QUESTION);

        }

        @Test
        @DisplayName("NoC questions successfully returned for a Solicitor user")
        void shouldReturnQuestionsForNocDetails() {
            UserInfo userInfo = new UserInfo("", "", "", "", "",
                                             Arrays.asList("caseworker-test", "caseworker-Jurisdiction-solicitor")
            );
            given(securityUtils.getUserInfo()).willReturn(userInfo);
            given(securityUtils.hasSolicitorAndJurisdictionRoles(anyList(), any())).willReturn(true);
            NoCRequestDetails noCRequestDetails = service.challengeQuestions(CASE_ID);

            assertThat(noCRequestDetails).isNotNull();
            assertThat(noCRequestDetails.getChallengeQuestionsResult().getQuestions()
                           .get(0).getCaseTypeId()).isEqualTo(CASE_TYPE_ID);
            assertThat(noCRequestDetails.getChallengeQuestionsResult().getQuestions()
                           .get(0).getOrder()).isEqualTo(1);
            assertThat(noCRequestDetails.getChallengeQuestionsResult().getQuestions()
                           .get(0).getQuestionText()).isEqualTo(QUESTION_TEXT);
            assertThat(noCRequestDetails.getChallengeQuestionsResult().getQuestions()
                           .get(0).getChallengeQuestionId())
                .isEqualTo(CHALLENGE_QUESTION);
            assertThat(noCRequestDetails.getCaseViewResource()).isNotNull();
            assertThat(noCRequestDetails.getOrganisationPolicy()).isNull();
            assertThat(noCRequestDetails.getCaseDetails().getId()).isEqualTo("1567934206391385");
        }

        @Test
        @DisplayName("NoC questions successfully returned for a Admin user")
        void shouldReturnQuestionsForAdmin() {
            ChallengeQuestionsResult challengeQuestionsResult = service.getChallengeQuestions(CASE_ID);

            assertThat(challengeQuestionsResult).isNotNull();
            assertThat(challengeQuestionsResult.getQuestions().get(0).getCaseTypeId()).isEqualTo(CASE_TYPE_ID);
            assertThat(challengeQuestionsResult.getQuestions().get(0).getOrder()).isEqualTo(1);
            assertThat(challengeQuestionsResult.getQuestions().get(0).getQuestionText()).isEqualTo(QUESTION_TEXT);
            assertThat(challengeQuestionsResult.getQuestions().get(0).getChallengeQuestionId())
                .isEqualTo(CHALLENGE_QUESTION);
        }

        @Test
        @DisplayName("must return an error response when a NoC request is currently pending on a case")
        void shouldThrowErrorNoCRequestPending() {

            Organisation organisationToAdd = Organisation.builder().organisationID("id1").build();
            Organisation organisationToRemove = Organisation.builder().organisationID("id2").build();

            DynamicListElement dynamicListElement = DynamicListElement.builder().code("code").label("label").build();
            DynamicList dynamicList =
                DynamicList.builder().value(dynamicListElement).listItems(List.of(dynamicListElement)).build();
            ChangeOrganisationRequest cor = ChangeOrganisationRequest.builder()
                .caseRoleId(dynamicList)
                .organisationToAdd(organisationToAdd)
                .organisationToRemove(organisationToRemove)
                .approvalStatus(PENDING.name())
                .build();

            JsonNode corNode = new ObjectMapper().convertValue(cor, JsonNode.class);
            Map<String, JsonNode> data = ImmutableMap.of("COR1", corNode);

            CaseDetails caseDetails = CaseDetails.builder().id(CASE_ID).data(data).build();

            // external case by id
            given(dataStoreRepository.findCaseByCaseIdAsSystemUserUsingExternalApi(CASE_ID)).willReturn(caseDetails);

            assertThatThrownBy(() -> service.getChallengeQuestions(CASE_ID))
                .isInstanceOf(NoCException.class)
                .hasMessageContaining(NOC_REQUEST_ONGOING);
        }

        @Test
        @DisplayName("must return an error response when there is more than one change request")
        void shouldThrowErrorMoreThanOneChangeRequest() {

            Organisation organisationToAdd = Organisation.builder().organisationID("id1").build();
            DynamicListElement dynamicListElement = DynamicListElement.builder().code("code").label("label").build();
            DynamicList dynamicList =
                DynamicList.builder().value(dynamicListElement).listItems(List.of(dynamicListElement)).build();

            ChangeOrganisationRequest cor = ChangeOrganisationRequest.builder()
                .caseRoleId(dynamicList)
                .organisationToAdd(organisationToAdd)
                .approvalStatus(PENDING.name())
                .build();

            JsonNode corNode = new ObjectMapper().convertValue(cor, JsonNode.class);
            Map<String, JsonNode> data = ImmutableMap.of("COR1", corNode, "COR2", corNode);

            CaseDetails caseDetails = CaseDetails.builder().id(CASE_ID).data(data).build();

            // external case by id
            given(dataStoreRepository.findCaseByCaseIdAsSystemUserUsingExternalApi(CASE_ID)).willReturn(caseDetails);

            assertThatThrownBy(() -> service.getChallengeQuestions(CASE_ID))
                .isInstanceOf(NoCException.class)
                .hasMessageContaining(CHANGE_REQUEST);
        }

        @Test
        @DisplayName("Must return an error if there is not an Organisation Policy field containing a "
            + "case role for each set of answers")
        void shouldThrowErrorMissingRoleInOrgPolicy() {
            FieldType fieldType = FieldType.builder()
                .max(null)
                .min(null)
                .id(FIELD_ID)
                .type(FIELD_ID)
                .build();
            ChallengeAnswer challengeAnswer = new ChallengeAnswer(ANSWER_FIELD_RESPONDENT);

            ChallengeQuestion challengeQuestion = ChallengeQuestion.builder()
                .caseTypeId(CASE_TYPE_ID)
                .challengeQuestionId("QuestionId1")
                .questionText(QUESTION_TEXT)
                .answerFieldType(fieldType)
                .answerField(ANSWER_FIELD)
                .answers(Arrays.asList(challengeAnswer))
                .questionId("NoC").build();
            ChallengeQuestionsResult challengeQuestionsResult =
                new ChallengeQuestionsResult(Arrays.asList(challengeQuestion));
            given(definitionStoreRepository
                      .challengeQuestions(CASE_TYPE_ID, "NoCChallenge"))
                .willReturn(challengeQuestionsResult);

            assertThatThrownBy(() -> service.getChallengeQuestions(CASE_ID))
                .isInstanceOf(NoCException.class)
                .hasMessageContaining(NO_ORG_POLICY_WITH_ROLE);
        }

        @Test
        @DisplayName("Must return an error when no NOC Request event is available on the case")
        void shouldThrowErrorNoEventAvailable() {
            CaseViewResource caseViewResource = new CaseViewResource();
            given(dataStoreRepository.findCaseByCaseId(CASE_ID))
                .willReturn(caseViewResource);

            assertThatThrownBy(() -> service.getChallengeQuestions(CASE_ID))
                .isInstanceOf(NoCException.class)
                .hasMessageContaining(NOC_EVENT_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("Must return an error when multiple NOC Request events are available in the case for the user")
        void shouldThrowErrorWhenMultipleNoCEventsAvailable() {
            CaseViewResource caseViewResource = new CaseViewResource();
            caseViewResource.setCaseViewActionableEvents(
                new CaseViewActionableEvent[]{new CaseViewActionableEvent(), new CaseViewActionableEvent()}
            );

            given(dataStoreRepository.findCaseByCaseId(CASE_ID))
                .willReturn(caseViewResource);

            assertThatThrownBy(() -> service.getChallengeQuestions(CASE_ID))
                .isInstanceOf(NoCException.class)
                .hasMessageContaining(MULTIPLE_NOC_REQUEST_EVENTS);
        }

        @Test
        @DisplayName(" must return an error response when the solicitor does not have access to the "
            + "jurisdiction of the case")
        void shouldThrowErrorInsufficientPrivilegesForSolicitor() {
            UserInfo userInfo = new UserInfo("", "", "", "", "",
                                             Arrays.asList("caseworker-JurisdictionA-solicitor")
            );
            given(securityUtils.getUserInfo()).willReturn(userInfo);

            assertThatThrownBy(() -> service.getChallengeQuestions(CASE_ID))
                .isInstanceOf(NoCException.class)
                .hasMessageContaining(INSUFFICIENT_PRIVILEGE);
        }

        @Test
        @DisplayName("Must return an error response when user has an invalid Solicitor role")
        void shouldThrowErrorInvalidSolicitorRole() {
            UserInfo userInfo = new UserInfo("", "", "", "", "",
                                             Arrays.asList("caseworker-test", "caseworker-Jurisdiction-solicit")
            );
            given(securityUtils.getUserInfo()).willReturn(userInfo);
            given(securityUtils.hasSolicitorAndJurisdictionRoles(anyList(), any())).willReturn(false);

            assertThatThrownBy(() -> service.getChallengeQuestions(CASE_ID))
                .isInstanceOf(NoCException.class)
                .hasMessageContaining(INSUFFICIENT_PRIVILEGE);
        }

        @Test
        @DisplayName("Must return an error response for case id which does not exist")
        void shouldThrowErrorInvalidCaseId() {
            UserInfo userInfo = new UserInfo("", "", "", "", "",
                                             Arrays.asList("caseworker-test", "caseworker-Jurisdiction-solicit")
            );
            given(securityUtils.getUserInfo()).willReturn(userInfo);
            given(securityUtils.hasSolicitorAndJurisdictionRoles(anyList(), any())).willReturn(true);
            given(dataStoreRepository.findCaseByCaseId(CASE_ID))
                .willThrow(new CaseCouldNotBeFoundException(CASE_NOT_FOUND));
            assertThatThrownBy(() -> service.getChallengeQuestions(CASE_ID))
                .isInstanceOf(CaseCouldNotBeFoundException.class)
                .hasMessageContaining(ValidationError.CASE_NOT_FOUND);
        }
    }
}
