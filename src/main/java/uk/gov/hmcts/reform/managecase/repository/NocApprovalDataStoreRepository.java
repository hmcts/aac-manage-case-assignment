package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
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
}
