package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.PrdApiClient;

@Repository
public class DefaultPrdRepository implements PrdRepository {

    private final PrdApiClient prdApi;

    @Autowired
    public DefaultPrdRepository(PrdApiClient prdApi) {
        this.prdApi = prdApi;
    }

    @Override
    @Cacheable(value = "usersByOrganisation", key = "@securityUtils.userToken")
    public FindUsersByOrganisationResponse findUsersByOrganisation() {
        return prdApi.findActiveUsersByOrganisation();
    }
}
