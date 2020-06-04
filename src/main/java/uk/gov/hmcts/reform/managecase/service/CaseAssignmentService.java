package uk.gov.hmcts.reform.managecase.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import java.util.List;

@Service
public class CaseAssignmentService {

    private final DataStoreRepository dataStoreRepository;
    private final PrdRepository prdRepository;
    private final SecurityUtils securityUtils;

    @Autowired
    public CaseAssignmentService(
        DataStoreRepository dataStoreRepository,
        PrdRepository prdRepository,
        SecurityUtils securityUtils
    ) {
        this.dataStoreRepository = dataStoreRepository;
        this.prdRepository = prdRepository;
        this.securityUtils = securityUtils;
    }

    @SuppressWarnings("PMD")
    public String assignCaseAccess(CaseAssignment assignment) {
        CaseDetails caseDetails = dataStoreRepository.findCaseBy(assignment.getCaseTypeId(), assignment.getCaseId());
        UserInfo invoker = securityUtils.getUserInfo();
        List<String> invokerRoles = invoker.getRoles();
        // TODO : all validation and invoke final assign data-store api
        // 1. IDAM admin role
        /* if (!(invokerRoles.contains("caseworker-caa") ||
            invokerRoles.contains("solicitor-" + caseDetails.getJurisdiction()))) {
            return "403 - The user is neither a case access administrator nor a solicitor"
                + " with access to the jurisdiction of the case.";

        } */
        // 2. check organization match

        String invokerOrganization = "sdds";

        List<ProfessionalUser> users = prdRepository.findUsersByOrganisation();

        // TODO : all validation and invoke final assign data-store api
        return "Success";
    }
}
