package uk.gov.hmcts.reform.managecase.api.controller.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController;
import uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController;
import uk.gov.hmcts.reform.managecase.api.errorhandling.RestExceptionHandler;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCException;
import uk.gov.hmcts.reform.managecase.api.payload.IdamUser;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewActionableEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewType;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.prd.FindOrganisationResponse;
import uk.gov.hmcts.reform.managecase.config.MapperConfig;
import uk.gov.hmcts.reform.managecase.data.user.UserRepository;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.noc.ChallengeAnswerValidator;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeQuestions;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.organisationPolicy;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.user;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.usersByOrganisation;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.ANSWERS_NOT_MATCH_LITIGANT;

@ExtendWith(SpringExtension.class)
@Provider("acc_manageCaseAssignment")
@PactBroker(url = "${PACT_BROKER_URL:http://localhost:80}")
@ContextConfiguration(classes = {ContractConfig.class, MapperConfig.class})
@TestPropertySource(locations = "/application.properties")
@IgnoreNoPactsToVerify
public class NocCaseAssignmentProviderTests {

    private static final String ORG_POLICY_ROLE = "caseworker-probate";
    private static final String ORG_POLICY_ROLE2 = "caseworker-probate2";
    private static final String ORG_POLICY_ROLE3 = "Role1";
    private static final String ORG_POLICY_ROLE4 = "Role2";
    private static final String ORGANIZATION_ID = "TEST_ORG";
    private static final String ASSIGNEE_ID = "0a5874a4-3f38-4bbd-ba4c";
    private static final String BEAR_TOKEN = "TestBearToken";

    private static final String ASSIGNEE_ID2 = "38130f09-0010-4c12-afd1-2563bb25d1d3";
    private static final String ASSIGNEE_ID3 = "userId";
    private static final String CASE_ID = "12345678";
    private static final String CASE_ID2 = "87654321";
    private static final String CASE_ROLE = "[CR1]";
    private static final String CASE_ROLE2 = "[CR2]";
    private static final String NOC_CASE_ID = "1234567812345670";
    private static final String NOC_CASE_ROLE = "[Claimant]";
    private static final String INVALID_NOC_CASE_ROLE = "[APPLICANT]";
    private static final String NOC_EVENT_ID = "noc-request";
    private static final String QUK_ORGANISATION_ID = "QUK822NA";

    private static final String TEST_APP_ORG_ID = "appOrgId";
    private static final String TEST_APP_ORG_NAME = "appOrgName";

    @Autowired
    DataStoreRepository dataStoreRepository;
    @Autowired
    PrdRepository prdRepository;
    @Autowired
    IdamRepository idamRepository;
    @Autowired
    JacksonUtils jacksonUtils;
    @Autowired
    SecurityUtils securityUtils;
    @Autowired
    NoticeOfChangeQuestions noticeOfChangeQuestions;
    @Autowired
    ChallengeAnswerValidator challengeAnswerValidator;
    @Autowired
    DefinitionStoreRepository definitionStoreRepository;
    @Autowired
    UserRepository userRepository;

    @Autowired
    CaseAssignmentController caseAssignmentController;

    @Autowired
    NoticeOfChangeController noticeOfChangeController;

    @PactBrokerConsumerVersionSelectors
    public SelectorBuilder consumerVersionSelectors() {
        return new SelectorBuilder().latestTag("Dev");
    }


    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        //System.getProperties().setProperty("pact.verifier.publishResults", "true");
        testTarget.setControllers(caseAssignmentController, noticeOfChangeController);
        testTarget.setControllerAdvices(new RestExceptionHandler());
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @State({"Assign a user to a case"})
    public void toAssignUserToCase() throws IOException {

        given(prdRepository.findUsersByOrganisation())
            .willReturn(usersByOrganisation(user(ASSIGNEE_ID), user(ASSIGNEE_ID2), user(ASSIGNEE_ID3)));

        //use_user_token=true mock
        given(dataStoreRepository.findCaseByCaseIdUsingExternalApi(TestFixtures.CASE_ID))
            .willReturn(TestFixtures.CaseDetailsFixture.caseDetails(ORGANIZATION_ID,  ORG_POLICY_ROLE3,
                                                                    ORG_POLICY_ROLE4));
        //use_user_token=false or not present mock
        given(dataStoreRepository.findCaseByCaseIdAsSystemUserUsingExternalApi(TestFixtures.CASE_ID))
            .willReturn(TestFixtures.CaseDetailsFixture.caseDetails(ORGANIZATION_ID, ORG_POLICY_ROLE3,
                                                                    ORG_POLICY_ROLE4));

        given(securityUtils.hasSolicitorRole(anyList())).willReturn(true);
        given(securityUtils.hasSolicitorAndJurisdictionRoles(anyList(), anyString())).willReturn(true);

        UserDetails userDetails = UserDetails.builder()
            .id(ASSIGNEE_ID).roles(List.of("caseworker-AUTOTEST1-solicitor")).build();
        given(idamRepository.getCaaSystemUserAccessToken()).willReturn(BEAR_TOKEN);
        given(idamRepository.getNocApproverSystemUserAccessToken()).willReturn(BEAR_TOKEN);
        given(idamRepository.getUserByUserId(ASSIGNEE_ID, BEAR_TOKEN)).willReturn(userDetails);
        given(idamRepository.getUserByUserId(ASSIGNEE_ID3, BEAR_TOKEN)).willReturn(userDetails);

        given(jacksonUtils.convertValue(any(JsonNode.class), eq(OrganisationPolicy.class)))
            .willReturn(organisationPolicy(ORGANIZATION_ID, ORG_POLICY_ROLE))
            .willReturn(organisationPolicy(ORGANIZATION_ID, ORG_POLICY_ROLE2));
    }

    @State({"Case assignments exist for case Ids"})
    public void toGetExistingCaseAssignments() throws IOException {
        given(prdRepository.findUsersByOrganisation())
            .willReturn(usersByOrganisation(user(ASSIGNEE_ID), user(ASSIGNEE_ID2)));
        given(dataStoreRepository.getCaseAssignments(eq(List.of(CASE_ID, CASE_ID2)),
            eq(List.of(ASSIGNEE_ID, ASSIGNEE_ID2))))
            .willReturn(List.of(
                new CaseUserRole(CASE_ID, ASSIGNEE_ID, CASE_ROLE),
                new CaseUserRole(CASE_ID2, ASSIGNEE_ID2, CASE_ROLE),
                new CaseUserRole(CASE_ID2, ASSIGNEE_ID2, CASE_ROLE2)));

    }

    @State({"A notice of change against case"})
    public void toApplyNoticeOfChange() throws IOException {
        given(dataStoreRepository.getCaseAssignments(any(), any()))
            .willReturn(List.of(
                new CaseUserRole(CASE_ID, ASSIGNEE_ID, CASE_ROLE),
                new CaseUserRole(CASE_ID2, ASSIGNEE_ID2, CASE_ROLE2)
            ));

        given(prdRepository.findUsersByOrganisation(anyString()))
            .willReturn(usersByOrganisation(user(ASSIGNEE_ID), user(ASSIGNEE_ID2)));

        when(prdRepository.findOrganisationAddress(any()))
            .thenReturn(new FindOrganisationResponse(emptyList(), TEST_APP_ORG_ID, TEST_APP_ORG_NAME));
    }

    @State("A valid NoC answers verification request")
    public void toVerifyValidNoCAnswers() {
        configureNoCAnswerVerification(QUK_ORGANISATION_ID);
    }

    @State("A valid submit NoC event is requested")
    public void toSubmitValidNoCRequest() {
        configureNoCAnswerVerification(QUK_ORGANISATION_ID);

        given(jacksonUtils.convertValue(any(JsonNode.class), eq(OrganisationPolicy.class)))
            .willReturn(organisationPolicyFor(QUK_ORGANISATION_ID))
            .willReturn(organisationPolicyFor(ORGANIZATION_ID));

        given(definitionStoreRepository.caseRoles(anyString(), anyString(), anyString()))
            .willReturn(List.of(CaseRole.builder().id(NOC_CASE_ROLE).name(NOC_CASE_ROLE).build()));
        given(userRepository.getUser()).willReturn(new IdamUser());
        given(dataStoreRepository.findCaseByCaseIdAsSystemUserUsingExternalApi(NOC_CASE_ID))
            .willReturn(nocCaseDetails(ORGANIZATION_ID, true));
        given(jacksonUtils.convertValue(any(JsonNode.class), eq(ChangeOrganisationRequest.class)))
            .willReturn(ChangeOrganisationRequest.builder().build());
    }

    @State("A NoC answer request with invalid case ID")
    public void toSubmitInvalidNoCRequest() {
        configureNoCAnswerVerification(QUK_ORGANISATION_ID);
        given(challengeAnswerValidator.getMatchingCaseRole(any(), any(), any()))
            .willReturn(INVALID_NOC_CASE_ROLE);
        given(jacksonUtils.convertValue(any(JsonNode.class), eq(OrganisationPolicy.class)))
            .willReturn(OrganisationPolicy.builder()
                .orgPolicyCaseAssignedRole(INVALID_NOC_CASE_ROLE)
                .organisation(new Organisation(QUK_ORGANISATION_ID, "Some Org"))
                .build());
        given(definitionStoreRepository.caseRoles(anyString(), anyString(), anyString()))
            .willReturn(emptyList());
    }

    @State("An invalid NoC answer request")
    public void toVerifyInvalidNoCAnswers() {
        given(noticeOfChangeQuestions.challengeQuestions(NOC_CASE_ID))
            .willReturn(noCRequestDetails(TEST_APP_ORG_ID));
        given(challengeAnswerValidator.getMatchingCaseRole(any(), any(), any()))
            .willThrow(new NoCException(ANSWERS_NOT_MATCH_LITIGANT));
    }

    private void configureNoCAnswerVerification(String organisationId) {
        given(noticeOfChangeQuestions.challengeQuestions(NOC_CASE_ID))
            .willReturn(noCRequestDetails(organisationId));
        given(challengeAnswerValidator.getMatchingCaseRole(any(), any(), any()))
            .willReturn(NOC_CASE_ROLE);
        given(jacksonUtils.convertValue(any(JsonNode.class), eq(OrganisationPolicy.class)))
            .willReturn(organisationPolicyFor(organisationId));
        given(prdRepository.findUsersByOrganisation())
            .willReturn(usersByOrganisation());
        given(securityUtils.getUserInfo())
            .willReturn(new UserInfo("user-id", "test@email.com", "Test", "User", "ACTIVE",
                                     List.of("caseworker-AUTOTEST1-solicitor")));
        given(securityUtils.hasSolicitorAndJurisdictionRoles(any(), anyString())).willReturn(true);
    }

    private NoCRequestDetails noCRequestDetails(String organisationId) {
        return NoCRequestDetails.builder()
            .caseViewResource(nocCaseViewResource())
            .caseDetails(nocCaseDetails(organisationId, false))
            .build();
    }

    private CaseViewResource nocCaseViewResource() {
        CaseViewType caseViewType = new CaseViewType();
        caseViewType.setId("PROBATE");
        CaseViewActionableEvent event = new CaseViewActionableEvent();
        event.setId(NOC_EVENT_ID);
        CaseViewResource caseViewResource = new CaseViewResource();
        caseViewResource.setReference(NOC_CASE_ID);
        caseViewResource.setCaseType(caseViewType);
        caseViewResource.setCaseViewActionableEvents(new CaseViewActionableEvent[]{event});
        return caseViewResource;
    }

    private CaseDetails nocCaseDetails(String organisationId, boolean includeChangeOrganisationRequest) {
        OrganisationPolicy policy = organisationPolicyFor(organisationId);
        Map<String, JsonNode> data = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        data.put("organisationPolicy", objectMapper.valueToTree(policy));
        if (includeChangeOrganisationRequest) {
            ChangeOrganisationRequest changeOrganisationRequest = ChangeOrganisationRequest.builder()
                .organisationToAdd(new Organisation(TEST_APP_ORG_ID, "Some Org"))
                .organisationToRemove(new Organisation("OLD_ORG", "Old Org"))
                .build();
            data.put("changeOrganisationRequest", objectMapper.valueToTree(changeOrganisationRequest));
        }
        return CaseDetails.builder()
            .id(NOC_CASE_ID)
            .caseTypeId("PROBATE")
            .jurisdiction("AUTOTEST1")
            .data(data)
            .build();
    }

    private OrganisationPolicy organisationPolicyFor(String organisationId) {
        return OrganisationPolicy.builder()
            .orgPolicyCaseAssignedRole(NOC_CASE_ROLE)
            .organisation(new Organisation(organisationId, "Some Org"))
            .build();
    }
}
