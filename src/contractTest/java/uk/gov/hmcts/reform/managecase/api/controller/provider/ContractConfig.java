package uk.gov.hmcts.reform.managecase.api.controller.provider;

import uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController;
import uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController;
import uk.gov.hmcts.reform.managecase.data.user.UserRepository;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;
import uk.gov.hmcts.reform.managecase.service.NotifyService;
import uk.gov.hmcts.reform.managecase.service.noc.ApplyNoCDecisionService;
import uk.gov.hmcts.reform.managecase.service.noc.ChallengeAnswerValidator;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeApprovalService;
import uk.gov.hmcts.reform.managecase.service.noc.NoticeOfChangeQuestions;
import uk.gov.hmcts.reform.managecase.service.noc.PrepareNoCService;
import uk.gov.hmcts.reform.managecase.service.noc.RequestNoticeOfChangeService;
import uk.gov.hmcts.reform.managecase.service.noc.VerifyNoCAnswersService;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Configuration
public class ContractConfig {

    @MockitoBean
    PrdRepository prdRepository;
    @MockitoBean
    DataStoreRepository dataStoreRepository;
    @MockitoBean
    IdamRepository idamRepository;
    @MockitoBean
    JacksonUtils jacksonUtils;
    @MockitoBean
    SecurityUtils securityUtils;
    @MockitoBean
    NoticeOfChangeQuestions noticeOfChangeQuestions;
    @MockitoBean
    ChallengeAnswerValidator challengeAnswerValidator;
    @MockitoBean
    DefinitionStoreRepository definitionStoreRepository;
    @MockitoBean
    UserRepository userRepository;
    @MockitoBean
    NotifyService notifyService;


    @Autowired
    ModelMapper modelMapper;

    @Bean
    @Primary
    public CaseAssignmentController caseAssignmentController() {
        return new CaseAssignmentController(caseAssignmentService(), modelMapper);
    }

    @Bean
    @Primary
    public NoticeOfChangeController noticeOfChangeController() {
        return new NoticeOfChangeController(noticeOfChangeQuestions,
                                            noticeOfChangeApprovalService(),
                                            verifyNoCAnswersService(),
                                            prepareNoCService(),
                                            requestNoticeOfChangeService(),
                                            applyNoCDecisionService(),
                                            jacksonUtils);
    }

    public CaseAssignmentService caseAssignmentService() {
        return new CaseAssignmentService(prdRepository,
                                         dataStoreRepository,
                                         idamRepository,
                                         jacksonUtils,
                                         securityUtils);
    }

    public NoticeOfChangeApprovalService noticeOfChangeApprovalService() {
        return new NoticeOfChangeApprovalService(dataStoreRepository);
    }

    public VerifyNoCAnswersService verifyNoCAnswersService() {
        return new VerifyNoCAnswersService(noticeOfChangeQuestions,
                                           challengeAnswerValidator,
                                           prdRepository,
                                           jacksonUtils);
    }

    public PrepareNoCService prepareNoCService() {
        return new PrepareNoCService(prdRepository,
                                     securityUtils,
                                     jacksonUtils,
                                     definitionStoreRepository);
    }

    public RequestNoticeOfChangeService requestNoticeOfChangeService() {
        return new RequestNoticeOfChangeService(noticeOfChangeQuestions,
                                                dataStoreRepository,
                                                definitionStoreRepository,
                                                prdRepository,
                                                jacksonUtils,
                                                securityUtils,
                                                userRepository);
    }

    public ApplyNoCDecisionService applyNoCDecisionService() {
        return new ApplyNoCDecisionService(prdRepository,
                                           dataStoreRepository,
                                           notifyService,
                                           jacksonUtils,
                                           new ObjectMapper());
    }
}
