package uk.gov.hmcts.reform.managecase.provider;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.managecase.Application;
import uk.gov.hmcts.reform.managecase.TestFixtures;
import uk.gov.hmcts.reform.managecase.TestIdamConfiguration;
import uk.gov.hmcts.reform.managecase.TestSecurityConfiguration;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignedUsers;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;

import javax.validation.ValidationException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = {
    Application.class, TestIdamConfiguration.class, TestSecurityConfiguration.class
})
@AutoConfigureWireMock(port = 0, stubs = "classpath:/wiremock-stubs")
@TestPropertySource("/contract-test.properties")

@ExtendWith(SpringExtension.class)
@Provider("mca")
@PactBroker
public class CaseAssignmentProviderTest {

    @LocalServerPort
    private int port;

    @MockBean
    protected CaseAssignmentService mockService;

    @Value("${pact.verifier.publishResults:false}")
    private String publishResults;

    @BeforeEach
    public void setup(PactVerificationContext context) {
        System.getProperties().setProperty("pact.verifier.publishResults", publishResults);
        context.setTarget(new HttpTestTarget("localhost", port, "/"));
    }

    @AfterEach
    public void teardown(PactVerificationContext context) {
        reset(mockService);
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    public void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("MCA successfully return case assignments")
    public void getCaseAssignments() {
        List<CaseAssignedUsers> caseAssignedUsers = List.of(TestFixtures.CaseAssignedUsersFixture.caseAssignedUsers());
        given(mockService.getCaseAssignments(anyList())).willReturn(caseAssignedUsers);
    }

    @State("MCA successfully assign case access")
    public void assignCaseAccess() {
        given(mockService.assignCaseAccess(any(CaseAssignment.class))).willReturn(List.of("[Collaborator]"));
    }

    @State("MCA throws validation error for assignCaseAccess")
    public void validationErrorForAssignCaseAccess() {
        given(mockService.assignCaseAccess(any(CaseAssignment.class)))
            .willThrow(new ValidationException(ValidationError.ASSIGNEE_ORGANISATION_ERROR));
    }

    @State("MCs successfully unAssign case access")
    public void unAssignCaseAccess() {
        doNothing().when(mockService).unassignCaseAccess(anyList());
    }
}


