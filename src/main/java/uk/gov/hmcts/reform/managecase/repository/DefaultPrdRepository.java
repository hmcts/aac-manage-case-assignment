package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.prd.FindOrganisationResponse;
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
    @Cacheable(value = "usersByOrganisationExternal", key = "@securityUtils.userToken")
    public FindUsersByOrganisationResponse findUsersByOrganisation() {
        return prdApi.findActiveUsersByOrganisation();
    }

    @Override
    @Cacheable("usersByOrganisationInternal")
    public FindUsersByOrganisationResponse findUsersByOrganisation(String organisationId) {
        return prdApi.findActiveUsersByOrganisation(organisationId);
    }

    @Override
    @Cacheable("organisationAddressById")
    public FindOrganisationResponse findOrganisationAddress(String organisationId) {
        return prdApi.findOrganisation(organisationId);
    }
}
