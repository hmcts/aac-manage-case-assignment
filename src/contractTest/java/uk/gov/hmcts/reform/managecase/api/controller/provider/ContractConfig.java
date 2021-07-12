package uk.gov.hmcts.reform.managecase.api.controller.provider;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.managecase.api.controller.CaseAssignmentController;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.CaseAssignmentService;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

@Configuration
public class ContractConfig {

    @MockBean
    PrdRepository prdRepository;
    @MockBean
    DataStoreRepository dataStoreRepository;
    @MockBean
    IdamRepository idamRepository;
    @MockBean
    JacksonUtils jacksonUtils;
    @MockBean
    SecurityUtils securityUtils;

    @Autowired
    ModelMapper modelMapper;

    @Bean
    @Primary
    public CaseAssignmentController caseAssignmentController() {
        return new CaseAssignmentController(caseAssignmentService(), modelMapper);
    }

    public CaseAssignmentService caseAssignmentService() {
        return new CaseAssignmentService(prdRepository, dataStoreRepository,
            idamRepository, jacksonUtils, securityUtils);
    }
}
