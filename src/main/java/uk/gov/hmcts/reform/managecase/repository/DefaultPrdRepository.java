package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.PrdApiClient;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;

import java.util.List;

@Repository
public class DefaultPrdRepository implements PrdRepository {

    public static final String ACTIVE = "Active";

    private final PrdApiClient prdApi;

    @Autowired
    public DefaultPrdRepository(PrdApiClient prdApi) {
        this.prdApi = prdApi;
    }

    @Override
    @Cacheable(value = "usersByOrganisation", key = "@securityUtils.userToken")
    public List<ProfessionalUser> findUsersByOrganisation() {
        FindUsersByOrganisationResponse apiResponse = prdApi.findUsersByOrganisation(ACTIVE);
        return apiResponse.getUsers();
    }
}
