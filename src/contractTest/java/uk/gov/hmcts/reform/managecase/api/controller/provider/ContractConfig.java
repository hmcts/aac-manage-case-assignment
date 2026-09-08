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
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ContractConfig {

    @Bean
    public PrdRepository prdRepository() {
        return Mockito.mock(PrdRepository.class);
    }

    @Bean
    public DataStoreRepository dataStoreRepository() {
        return Mockito.mock(DataStoreRepository.class);
    }

    @Bean
    public IdamRepository idamRepository() {
        return Mockito.mock(IdamRepository.class);
    }

    @Bean
    public JacksonUtils jacksonUtils() {
        return Mockito.mock(JacksonUtils.class);
    }

    @Bean
    public SecurityUtils securityUtils() {
        return Mockito.mock(SecurityUtils.class);
    }

    @Bean
    public NoticeOfChangeQuestions noticeOfChangeQuestions() {
        return Mockito.mock(NoticeOfChangeQuestions.class);
    }

    @Bean
    public ChallengeAnswerValidator challengeAnswerValidator() {
        return Mockito.mock(ChallengeAnswerValidator.class);
    }

    @Bean
    public DefinitionStoreRepository definitionStoreRepository() {
        return Mockito.mock(DefinitionStoreRepository.class);
    }

    @Bean
    public UserRepository userRepository() {
        return Mockito.mock(UserRepository.class);
    }

    @Bean
    public NotifyService notifyService() {
        return Mockito.mock(NotifyService.class);
    }

    @Bean
    @Primary
    public CaseAssignmentController caseAssignmentController(CaseAssignmentService caseAssignmentService,
                                                              ModelMapper modelMapper) {
        return new CaseAssignmentController(caseAssignmentService, modelMapper);
    }

    @Bean
    @Primary
    public NoticeOfChangeController noticeOfChangeController(
        NoticeOfChangeQuestions noticeOfChangeQuestions,
        NoticeOfChangeApprovalService noticeOfChangeApprovalService,
        VerifyNoCAnswersService verifyNoCAnswersService,
        PrepareNoCService prepareNoCService,
        RequestNoticeOfChangeService requestNoticeOfChangeService,
        ApplyNoCDecisionService applyNoCDecisionService,
        JacksonUtils jacksonUtils) {
        return new NoticeOfChangeController(noticeOfChangeQuestions,
                                            noticeOfChangeApprovalService,
                                            verifyNoCAnswersService,
                                            prepareNoCService,
                                            requestNoticeOfChangeService,
                                            applyNoCDecisionService,
                                            jacksonUtils);
    }

    @Bean
    public CaseAssignmentService caseAssignmentService() {
        return new CaseAssignmentService(prdRepository(),
                                         dataStoreRepository(),
                                         idamRepository(),
                                         jacksonUtils(),
                                         securityUtils());
    }

    @Bean
    public NoticeOfChangeApprovalService noticeOfChangeApprovalService() {
        return new NoticeOfChangeApprovalService(dataStoreRepository());
    }

    @Bean
    public VerifyNoCAnswersService verifyNoCAnswersService() {
        return new VerifyNoCAnswersService(noticeOfChangeQuestions(),
                                           challengeAnswerValidator(),
                                           prdRepository(),
                                           jacksonUtils());
    }

    @Bean
    public PrepareNoCService prepareNoCService() {
        return new PrepareNoCService(prdRepository(),
                                     securityUtils(),
                                     jacksonUtils(),
                                     definitionStoreRepository());
    }

    @Bean
    public RequestNoticeOfChangeService requestNoticeOfChangeService() {
        return new RequestNoticeOfChangeService(noticeOfChangeQuestions(),
                                                dataStoreRepository(),
                                                definitionStoreRepository(),
                                                prdRepository(),
                                                jacksonUtils(),
                                                securityUtils(),
                                                userRepository());
    }

    @Bean
    public ApplyNoCDecisionService applyNoCDecisionService() {
        return new ApplyNoCDecisionService(prdRepository(),
                                           dataStoreRepository(),
                                           notifyService(),
                                           jacksonUtils(),
                                           new ObjectMapper());
    }
}
