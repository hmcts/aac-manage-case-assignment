package uk.gov.hmcts.reform.managecase.api.controller.provider;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.prd.FindOrganisationResponse;
import uk.gov.hmcts.reform.managecase.config.MapperConfig;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.user;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.usersByOrganisation;

@ExtendWith(SpringExtension.class)
@Provider("acc_manageCaseAssignment")
@PactBroker(scheme = "${PACT_BROKER_SCHEME:http}",
    host = "${PACT_BROKER_URL:localhost}",
    port = "${PACT_BROKER_PORT:80}",
    consumerVersionSelectors = {
        @VersionSelector(tag = "master")
    }
)
@ContextConfiguration(classes = {ContractConfig.class, MapperConfig.class})
@IgnoreNoPactsToVerify
public class NoticeOfChangeControllerProviderTest {

    private static final String TEST_APP_ORG_ID = "appOrgId";
    private static final String TEST_APP_ORG_NAME = "appOrgName";
    private static final String ASSIGNEE_ID = "0a5874a4-3f38-4bbd-ba4c";
    private static final String ASSIGNEE_ID2 = "38130f09-0010-4c12-afd1-2563bb25d1d3";
    private static final String CASE_ID = "1583841721773828";
    private static final String CASE_ID2 = "87654321";
    private static final String CASE_ROLE = "[CR1]";
    private static final String CASE_ROLE2 = "[CR2]";

    @Autowired
    DataStoreRepository dataStoreRepository;

    @Autowired
    PrdRepository prdRepository;

    @Autowired
    NoticeOfChangeController noticeOfChangeController;

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
        testTarget.setControllers(noticeOfChangeController);
        if (context != null) {
            context.setTarget(testTarget);
        }
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
}
