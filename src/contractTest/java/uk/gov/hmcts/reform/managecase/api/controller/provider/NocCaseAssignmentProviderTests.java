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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController;
import uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.reform.managecase.config.MapperConfig;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeQuestions;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
@Provider("acc_manageCaseAssignment_Noc")
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
    CaseAssignmentController caseAssignmentController;

    @Autowired
    NoticeOfChangeController noticeOfChangeController;

    @PactBrokerConsumerVersionSelectors
    public static SelectorBuilder consumerVersionSelectors() {
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
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @State("NoC questions exist for case with given id")
    public void toGetNoCQuestions() {
        given(noticeOfChangeQuestions.getChallengeQuestions(anyString()))
            .willReturn(ChallengeQuestionsResult.builder()
                .questions(List.of(
                    ChallengeQuestion.builder()
                        .caseTypeId("FT_NoCCaseType")
                        .order(6)
                        .questionText("What's the name of the party you wish to represent?")
                        .answerField("")
                        .answerFieldType(FieldType.builder().id("Text").type("Text")
                            .min("0").max("10").regularExpression("asdsa")
                            .fixedListItems(emptyList()).complexFields(emptyList()).build())
                        .challengeQuestionId("NoCChallenge")
                        .questionId("NoC_Challenge_Name")
                        .displayContextParameter("1")
                        .ignoreNullFields(true)
                        .build(),
                    ChallengeQuestion.builder()
                        .caseTypeId("FT_NoCCaseType")
                        .order(7)
                        .questionText("significant date?")
                        .answerField("")
                        .answerFieldType(FieldType.builder().id("Date").type("Date")
                            .min("0").max("10").regularExpression("asdsa")
                            .fixedListItems(emptyList()).complexFields(emptyList()).build())
                        .displayContextParameter("#DATETIMEENTRY(dd-MM-yyyy)")
                        .challengeQuestionId("NoCChallenge")
                        .questionId("NoC_Challenge_Name2")
                        .build()))
                .build());
    }
}
