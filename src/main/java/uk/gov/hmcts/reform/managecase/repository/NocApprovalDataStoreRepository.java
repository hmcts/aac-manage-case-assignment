package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDataContent;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseResource;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseUpdateViewEvent;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

@Repository("nocApprovalDataStoreRepository")
public class NocApprovalDataStoreRepository extends DefaultDataStoreRepository {

    @Autowired
    public NocApprovalDataStoreRepository(DataStoreApiClient dataStoreApi,
                                          JacksonUtils jacksonUtils, SecurityUtils securityUtils) {
        super(dataStoreApi, jacksonUtils, securityUtils);
    }

    @Override
    protected String getUserAuthToken() {
        return securityUtils.getNocApproverSystemUserAccessToken();
    }

    @Override
    public CaseUpdateViewEvent getStartEventTrigger(String caseId, String eventId) {
        String userAuthToken = getUserAuthToken();
        return dataStoreApi.getStartEventTrigger(userAuthToken, caseId, eventId);
    }

    @Override
    public CaseResource submitEventForCaseOnly(String caseId, CaseDataContent caseDataContent) {
        String userAuthToken = getUserAuthToken();
        return dataStoreApi.submitEventForCase(userAuthToken, caseId, caseDataContent);
    }
}
