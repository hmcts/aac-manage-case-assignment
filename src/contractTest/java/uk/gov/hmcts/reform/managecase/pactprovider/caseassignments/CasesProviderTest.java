package uk.gov.hmcts.reform.managecase.pactprovider.caseassignments;

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
import org.mockito.Mock;
import org.modelmapper.ModelMapper;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.domain.UserDetails;
import uk.gov.hmcts.reform.managecase.pactprovider.caseassignments.controller.CasesRestController;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignedUsers;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Provider("acc_manageCaseAssignment")
@PactBroker(
    url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "master")})
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
public class CasesProviderTest {

    @Mock
    private CaseAssignmentService caseAssignmentService;

    @Mock
    private ModelMapper mapper;

    @BeforeEach
    void beforeCreate(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new CasesRestController(caseAssignmentService, mapper));
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @State("Case assignments exist for case Ids")
    public void getCaseAssignments() {
        when(caseAssignmentService.getCaseAssignments(anyList())).thenReturn(mockCaseAssignmentsList());
    }

    @State("Assign a user to a case")
    public void assignAccessWithinOrganisation() {
        when(mapper.map(any(Object.class), eq(CaseAssignment.class))).thenReturn(new CaseAssignment());
        when(caseAssignmentService.assignCaseAccess(any(CaseAssignment.class), anyBoolean()))
            .thenReturn(List.of("Role1","Role2"));
    }

    public List<CaseAssignedUsers> mockCaseAssignmentsList() {
        UserDetails userDetails = new UserDetails("221a2877-e1ab-4dc4-a9ff-f9424ad58738", "Bill",
                                                  "Roberts", "bill.roberts@greatbrsolicitors.co.uk",
                                                  List.of("[Claimant]", "[Defendant]"));
        CaseAssignedUsers caseAssignedUser = new CaseAssignedUsers("1588234985453946", List.of(userDetails));
        return List.of(caseAssignedUser);
    }
}
