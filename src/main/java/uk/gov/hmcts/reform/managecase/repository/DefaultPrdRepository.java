package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.PrdApiClient;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;

import java.util.Optional;

@Repository
public class DefaultPrdRepository implements PrdRepository {

    public static final String ACTIVE = "Active";

    private final PrdApiClient prdApi;

    @Autowired
    public DefaultPrdRepository(PrdApiClient prdApi) {
        this.prdApi = prdApi;
    }

    @Override
    public Optional<ProfessionalUser> findUserBy(String userIdentifier) {
        FindUsersByOrganisationResponse apiResponse = prdApi.findUsersByOrganisation(ACTIVE);
        return apiResponse.getUsers().stream()
                .filter(user -> userIdentifier.equalsIgnoreCase(user.getUserIdentifier()))
                .findFirst();
    }

}
