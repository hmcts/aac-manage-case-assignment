package uk.gov.hmcts.reform.managecase.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

@Service
public class CaseAssignmentService {

    private final DataStoreRepository dataStoreRepository;
    private final SecurityUtils securityUtils;

    @Autowired
    public CaseAssignmentService(DataStoreRepository dataStoreRepository, SecurityUtils securityUtils) {
        this.dataStoreRepository = dataStoreRepository;
        this.securityUtils = securityUtils;
    }

    @SuppressWarnings("PMD")
    public String assignCaseAccess(final CaseAssignment caseAssignment) {
        CaseDetails caseDetails = dataStoreRepository.findCaseById(caseAssignment.getCaseId());
        UserInfo invoker = securityUtils.getUserInfo();
        // TODO : all validation and invoke final assign data-store api
        return "Success";
    }
}
