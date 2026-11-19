package uk.gov.hmcts.reform.managecase.repository;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.prd.FindOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.PrdApiClient;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

@Repository
public class DefaultPrdRepository implements PrdRepository {

    private final PrdApiClient prdApi;
    private final SecurityUtils securityUtils;

    @Autowired
    public DefaultPrdRepository(PrdApiClient prdApi, SecurityUtils securityUtils) {
        this.prdApi = prdApi;
        this.securityUtils = securityUtils;
    }

    @Override
    @Cacheable(value = "requestingUserOrganisation", key = "@securityUtils.userToken")
    public FindOrganisationResponse findRequestingUserOrganisation() {
        try {
            return prdApi.findRequestingUserOrganisation();
        } catch (FeignException.NotFound e) {
            // Older PRD deployments do not expose the metadata endpoint yet.
            String userId = securityUtils.getUserId();
            if (userId == null || userId.isBlank()) {
                throw new IllegalStateException("Authenticated user ID is required for the organisation lookup", e);
            }
            FindUsersByOrganisationResponse response = prdApi.findActiveUserOrganisation(userId);
            return new FindOrganisationResponse(null, response.getOrganisationIdentifier(), null);
        }
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
