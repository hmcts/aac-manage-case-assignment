package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.managecase.BaseIT;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.StartEventResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseAccessMetadataResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseUpdateViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewActionableEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewJurisdiction;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewType;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.domain.SubmittedChallengeAnswer;
import uk.gov.hmcts.reform.managecase.repository.CaseTypeDefinitionVersion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseUpdateViewEventFixture.getCaseViewFields;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseUpdateViewEventFixture.getWizardPages;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_PATH;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient.CASE_USERS;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.APPROVED;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseInternal;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseRoles;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetCaseViaExternalApi;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetChallengeQuestions;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetExternalStartEventTrigger;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetLatestVersion;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubGetStartEventTrigger;
import static uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures.stubSubmitEventForCase;

@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert", "PMD.MethodNamingConventions",
    "PMD.AvoidDuplicateLiterals", "PMD.ExcessiveImports", "PMD.TooManyMethods",
    "PMD.UseConcurrentHashMap", "PMD.DataflowAnomalyAnalysis", "squid:S100", "squid:S1192"})
@DisplayName("POST /noc/noc-requests - auto-assignment of case roles")
public class NoticeOfChangeAutoAssignmentIT extends BaseIT {

    private static final String CASE_ID = "1588234985453946";
    private static final String CASE_TYPE_ID = "caseType";
    private static final String JURISDICTION = "Jurisdiction";

    // Matches the organisationIdentifier returned by wiremock-stubs/prd/prd_users.json.
    private static final String INVOKERS_ORG_ID = "InvokingUsersOrg";
    // Org used to anchor the challenge-question answer for the requested role.
    private static final String INCUMBENT_ORG_ID = "QUK822N";
    private static final String INCUMBENT_ORG_NAME = "CCD Solicitors Limited";

    private static final String NOC = "NOC";
    private static final String QUESTION_ID_1 = "QuestionId1";
    private static final String APPLICANT_ROLE = "Applicant";
    private static final String DEFENDANT_ROLE = "Defendant";

    private static final String ENDPOINT_URL = "/noc" + REQUEST_NOTICE_OF_CHANGE_PATH;

    private static final String SOLICITOR_UID = "solicitorUid";

    private static final String SOLICITOR_USER_INFO_JSON = "{"
        + "\"sub\":\"solicitor@hmcts.net\","
        + "\"uid\":\"" + SOLICITOR_UID + "\","
        + "\"roles\":[\"caseworker-" + JURISDICTION + "\",\"caseworker-" + JURISDICTION + "-solicitor\"],"
        + "\"name\":\"Sol Sol\","
        + "\"given_name\":\"Sol\","
        + "\"family_name\":\"Sol\""
        + "}";

    private static final String NON_SOLICITOR_USER_INFO_JSON = "{"
        + "\"sub\":\"caa@hmcts.net\","
        + "\"uid\":\"caaUid\","
        + "\"roles\":[\"caseworker-test\",\"pui-caa\"],"
        + "\"name\":\"Cee Aaa\","
        + "\"given_name\":\"Cee\","
        + "\"family_name\":\"Aaa\""
        + "}";

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        resetAllRequests();
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
            .id("Number").type("Number").build();
        ChallengeQuestion challengeQuestion = ChallengeQuestion.builder()
            .caseTypeId(CASE_TYPE_ID)
            .challengeQuestionId("NoC")
            .questionText("questionText")
            .answerFieldType(fieldType)
            .answerField("${applicant.individual.fullname}|${applicant.company.name}|"
                + "${applicant.soletrader.name}|${OrganisationPolicy1.Organisation.OrganisationID}:" + APPLICANT_ROLE)
            .questionId(QUESTION_ID_1)
            .order(1).build();
        stubGetChallengeQuestions(CASE_TYPE_ID, "NoCChallenge",
            new ChallengeQuestionsResult(List.of(challengeQuestion)));

        CaseTypeDefinitionVersion caseTypeDefinitionVersion = new CaseTypeDefinitionVersion();
        caseTypeDefinitionVersion.setVersion(3);
        stubGetLatestVersion(CASE_TYPE_ID, caseTypeDefinitionVersion);

        List<CaseRole> caseRoleList = List.of(
            CaseRole.builder().id(APPLICANT_ROLE).name(APPLICANT_ROLE).description(APPLICANT_ROLE).build(),
            CaseRole.builder().id(DEFENDANT_ROLE).name(DEFENDANT_ROLE).description(DEFENDANT_ROLE).build()
        );
        stubGetCaseRoles("0", "0", CASE_TYPE_ID, caseRoleList);

        CaseUpdateViewEvent caseUpdateViewEvent = CaseUpdateViewEvent.builder()
            .wizardPages(getWizardPages("changeOrganisationRequestField"))
            .eventToken("EVENT_TOKEN")
            .caseFields(getCaseViewFields())
            .build();
        stubGetStartEventTrigger(CASE_ID, NOC, caseUpdateViewEvent);

        CaseDetails submitResponse = CaseDetails.builder()
            .id(CASE_ID).caseTypeId(CASE_TYPE_ID).data(new LinkedHashMap<>()).build();
        stubSubmitEventForCase(CASE_ID, submitResponse);

        StartEventResource startEventResource = StartEventResource.builder()
            .token("token")
            .caseDetails(CaseDetails.builder().data(new LinkedHashMap<>()).build())
            .build();
        stubGetExternalStartEventTrigger(CASE_ID, NOC, startEventResource);

        stubAssignCaseEndpoint();
    }

    @Test
    @DisplayName("Single-role same-org: solicitor whose firm owns exactly one policy gets only that role assigned")
    void shouldAssignSingleRoleWhenSolicitorOwnsOnePolicyOnCase() throws Exception {
        stubSolicitorUserInfo();
        stubAccessMetadata(GrantType.SPECIFIC);
        stubPostCallbackCaseReload(false);

        mockMvc.perform(post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(applicantNoCRequest())))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.status_message", is(REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)))
            .andExpect(jsonPath("$.approval_status", is(APPROVED.name())));

        verify(exactly(1), postRequestedFor(urlEqualTo(CASE_USERS))
            .withRequestBody(matchingJsonPath("$.case_users.length()", equalTo("1")))
            .withRequestBody(matchingJsonPath("$.case_users[0].case_id", equalTo(CASE_ID)))
            .withRequestBody(matchingJsonPath("$.case_users[0].case_role", equalTo(APPLICANT_ROLE)))
            .withRequestBody(matchingJsonPath("$.case_users[0].user_id", equalTo(SOLICITOR_UID)))
            .withRequestBody(matchingJsonPath("$.case_users[0].organisation_id", equalTo(INVOKERS_ORG_ID))));
    }

    @Test
    @DisplayName("Multi-policy same-org: solicitor whose firm owns multiple policies gets only the "
        + "requested role assigned")
    void shouldAssignOnlyRequestedRoleWhenSolicitorOwnsMultiplePoliciesOnCase() throws Exception {
        stubSolicitorUserInfo();
        stubAccessMetadata(GrantType.SPECIFIC);
        stubPostCallbackCaseReload(true);

        mockMvc.perform(post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(applicantNoCRequest())))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.approval_status", is(APPROVED.name())));

        verify(exactly(1), postRequestedFor(urlEqualTo(CASE_USERS))
            .withRequestBody(matchingJsonPath("$.case_users.length()", equalTo("1")))
            .withRequestBody(matchingJsonPath("$.case_users[0].case_id", equalTo(CASE_ID)))
            .withRequestBody(matchingJsonPath("$.case_users[0].case_role", equalTo(APPLICANT_ROLE)))
            .withRequestBody(matchingJsonPath("$.case_users[0].user_id", equalTo(SOLICITOR_UID)))
            .withRequestBody(matchingJsonPath("$.case_users[0].organisation_id", equalTo(INVOKERS_ORG_ID))));
    }

    @Test
    @DisplayName("Duplicate-policy same-org: requested role is assigned exactly once when the firm owns "
        + "duplicate policies for that role")
    void shouldAssignRequestedRoleOnceWhenFirmOwnsDuplicatePoliciesForSameRole() throws Exception {
        stubSolicitorUserInfo();
        stubAccessMetadata(GrantType.SPECIFIC);
        stubPostCallbackCaseReloadWithDuplicateApplicantPolicy();

        mockMvc.perform(post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(applicantNoCRequest())))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.approval_status", is(APPROVED.name())));

        verify(exactly(1), postRequestedFor(urlEqualTo(CASE_USERS))
            .withRequestBody(matchingJsonPath("$.case_users.length()", equalTo("1")))
            .withRequestBody(matchingJsonPath("$.case_users[0].case_id", equalTo(CASE_ID)))
            .withRequestBody(matchingJsonPath("$.case_users[0].case_role", equalTo(APPLICANT_ROLE)))
            .withRequestBody(matchingJsonPath("$.case_users[0].user_id", equalTo(SOLICITOR_UID)))
            .withRequestBody(matchingJsonPath("$.case_users[0].organisation_id", equalTo(INVOKERS_ORG_ID))));
    }

    @Test
    @DisplayName("STANDARD access metadata prevents auto-assignment even when auto-approval completes")
    void shouldNotAssignWhenAccessMetadataIndicatesStandardAccess() throws Exception {
        stubSolicitorUserInfo();
        stubAccessMetadata(GrantType.STANDARD);
        stubPostCallbackCaseReload(false);

        mockMvc.perform(post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(applicantNoCRequest())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.approval_status", is(APPROVED.name())));

        verify(exactly(0), postRequestedFor(urlEqualTo(CASE_USERS)));
    }

    @Test
    @DisplayName("Non-solicitor invoker: auto-approval completes but no case-roles are assigned")
    void shouldNotAssignWhenInvokerIsNotActingAsSolicitor() throws Exception {
        stubNonSolicitorUserInfo();
        stubAccessMetadata(GrantType.SPECIFIC);
        stubPostCallbackCaseReload(false);

        mockMvc.perform(post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(applicantNoCRequest())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.approval_status", is(APPROVED.name())));

        verify(exactly(0), postRequestedFor(urlEqualTo(CASE_USERS)));
    }

    private RequestNoticeOfChangeRequest applicantNoCRequest() {
        SubmittedChallengeAnswer answer = new SubmittedChallengeAnswer(
            QUESTION_ID_1, INCUMBENT_ORG_ID.toLowerCase(Locale.getDefault()));
        return new RequestNoticeOfChangeRequest(CASE_ID, singletonList(answer));
    }

    private void stubPostCallbackCaseReload(boolean firmAlreadyHasAnotherRole) {
        ChangeOrganisationRequest postCallbackCor = ChangeOrganisationRequest.builder()
            .organisationToAdd(Organisation.builder().organisationID(INVOKERS_ORG_ID).build())
            .organisationToRemove(Organisation.builder().organisationID(INCUMBENT_ORG_ID).build())
            .createdBy("CreatedByUser")
            .build();

        OrganisationPolicy applicantPolicy = OrganisationPolicy.builder()
            .orgPolicyCaseAssignedRole(APPLICANT_ROLE)
            .orgPolicyReference("ApplicantPolicy")
            .organisation(new Organisation(INVOKERS_ORG_ID, null))
            .build();

        OrganisationPolicy incumbentApplicantPolicyForAnswerAnchor = OrganisationPolicy.builder()
            .orgPolicyCaseAssignedRole(APPLICANT_ROLE)
            .orgPolicyReference("ApplicantPolicy")
            .organisation(new Organisation(INCUMBENT_ORG_ID, INCUMBENT_ORG_NAME))
            .build();

        Map<String, JsonNode> caseFields = new LinkedHashMap<>();
        caseFields.put("OrganisationPolicy1", mapper.convertValue(incumbentApplicantPolicyForAnswerAnchor,
            JsonNode.class));
        caseFields.put("ChangeOrgRequest", mapper.convertValue(postCallbackCor, JsonNode.class));
        caseFields.put("OrganisationPolicy", mapper.convertValue(applicantPolicy, JsonNode.class));

        if (firmAlreadyHasAnotherRole) {
            OrganisationPolicy defendantPolicySameFirm = OrganisationPolicy.builder()
                .orgPolicyCaseAssignedRole(DEFENDANT_ROLE)
                .orgPolicyReference("DefendantPolicy")
                .organisation(new Organisation(INVOKERS_ORG_ID, null))
                .build();
            caseFields.put("OrganisationPolicy2",
                mapper.convertValue(defendantPolicySameFirm, JsonNode.class));
        }

        CaseDetails reloaded = CaseDetails.builder()
            .id(CASE_ID).caseTypeId(CASE_TYPE_ID).jurisdiction(JURISDICTION).data(caseFields).build();
        stubGetCaseViaExternalApi(CASE_ID, reloaded);
    }

    private void stubPostCallbackCaseReloadWithDuplicateApplicantPolicy() {
        ChangeOrganisationRequest postCallbackCor = ChangeOrganisationRequest.builder()
            .organisationToAdd(Organisation.builder().organisationID(INVOKERS_ORG_ID).build())
            .organisationToRemove(Organisation.builder().organisationID(INCUMBENT_ORG_ID).build())
            .createdBy("CreatedByUser")
            .build();

        OrganisationPolicy applicantPolicy = OrganisationPolicy.builder()
            .orgPolicyCaseAssignedRole(APPLICANT_ROLE)
            .orgPolicyReference("ApplicantPolicy")
            .organisation(new Organisation(INVOKERS_ORG_ID, null))
            .build();

        OrganisationPolicy duplicateApplicantPolicySameFirm = OrganisationPolicy.builder()
            .orgPolicyCaseAssignedRole(APPLICANT_ROLE)
            .orgPolicyReference("ApplicantPolicyDuplicate")
            .organisation(new Organisation(INVOKERS_ORG_ID, null))
            .build();

        OrganisationPolicy incumbentApplicantPolicyForAnswerAnchor = OrganisationPolicy.builder()
            .orgPolicyCaseAssignedRole(APPLICANT_ROLE)
            .orgPolicyReference("ApplicantPolicy")
            .organisation(new Organisation(INCUMBENT_ORG_ID, INCUMBENT_ORG_NAME))
            .build();

        Map<String, JsonNode> caseFields = new LinkedHashMap<>();
        caseFields.put("OrganisationPolicy1", mapper.convertValue(incumbentApplicantPolicyForAnswerAnchor,
            JsonNode.class));
        caseFields.put("ChangeOrgRequest", mapper.convertValue(postCallbackCor, JsonNode.class));
        caseFields.put("OrganisationPolicy", mapper.convertValue(applicantPolicy, JsonNode.class));
        caseFields.put("OrganisationPolicy2",
            mapper.convertValue(duplicateApplicantPolicySameFirm, JsonNode.class));

        CaseDetails reloaded = CaseDetails.builder()
            .id(CASE_ID).caseTypeId(CASE_TYPE_ID).jurisdiction(JURISDICTION).data(caseFields).build();
        stubGetCaseViaExternalApi(CASE_ID, reloaded);
    }

    private void stubSolicitorUserInfo() {
        stubFor(WireMock.get(urlPathEqualTo("/o/userinfo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(SOLICITOR_USER_INFO_JSON)));
    }

    private void stubNonSolicitorUserInfo() {
        stubFor(WireMock.get(urlPathEqualTo("/o/userinfo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(NON_SOLICITOR_USER_INFO_JSON)));
    }

    private void stubAccessMetadata(GrantType grantType) {
        CaseAccessMetadataResource resource = CaseAccessMetadataResource.builder()
            .accessGrants(List.of(grantType))
            .accessProcess(grantType.name())
            .build();
        stubFor(WireMock.get(urlPathEqualTo("/internal/cases/" + CASE_ID + "/access-metadata"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(getJsonString(resource))));
    }

    private void stubAssignCaseEndpoint() {
        stubFor(WireMock.post(urlEqualTo(CASE_USERS))
            .willReturn(aResponse().withStatus(200)));
    }

    private String getJsonString(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
