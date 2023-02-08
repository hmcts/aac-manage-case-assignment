package uk.gov.hmcts.reform.managecase.api.controller.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.config.MapperConfig;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.organisationPolicy;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.user;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.usersByOrganisation;

@ExtendWith(SpringExtension.class)
@Provider("acc_manageCaseAssignment")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:https}",
    host = "${PACT_BROKER_URL:pact-broker.platform.hmcts.net}",
    port = "${PACT_BROKER_PORT:443}",
    consumerVersionSelectors = {@VersionSelector(tag = "master")})
@ContextConfiguration(classes = {ContractConfig.class, MapperConfig.class})
@IgnoreNoPactsToVerify
public class CasesAssignmentControllerProviderTest {

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
    CaseAssignmentController caseAssignmentController;


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
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        testTarget.setControllers(caseAssignmentController);
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
}
