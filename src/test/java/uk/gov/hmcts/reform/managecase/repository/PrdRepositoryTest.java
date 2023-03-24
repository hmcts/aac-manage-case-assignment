package uk.gov.hmcts.reform.managecase.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.managecase.client.prd.FindOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.PrdApiClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ORGANIZATION_ID;

@ExtendWith(MockitoExtension.class)
class PrdRepositoryTest {

    public static final String USER_AUTH_TOKEN = "Bearer user Token";

    @Mock
    private PrdApiClient apiClient;

    @InjectMocks
    private DefaultPrdRepository repository;

    @Test
    @DisplayName("Find users in the invokers organisation")
    void shouldFindUsersByOrganisation_forInvokersOrganisation() {

        // GIVEN
        FindUsersByOrganisationResponse response = new FindUsersByOrganisationResponse(null, null);
        given(apiClient.findActiveUsersByOrganisation()).willReturn(response);

        // WHEN
        FindUsersByOrganisationResponse result = repository.findUsersByOrganisation();

        // THEN
        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("Find users in a given organisation")
    void shouldFindUsersByOrganisation_forAGivenOrganisationId() {

        // GIVEN
        FindUsersByOrganisationResponse response = new FindUsersByOrganisationResponse(null, null);
        given(apiClient.findActiveUsersByOrganisation(ORGANIZATION_ID)).willReturn(response);

        // WHEN
        FindUsersByOrganisationResponse result = repository.findUsersByOrganisation(ORGANIZATION_ID);

        // THEN
        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("Find users in a given organisation using specific auth header")
    void shouldFindUsersByOrganisation_forAGivenOrganisationId_usingSpecificAuthHeader() {

        // GIVEN
        FindUsersByOrganisationResponse response = new FindUsersByOrganisationResponse(null, null);
        given(apiClient.findActiveUsersByOrganisation(USER_AUTH_TOKEN, ORGANIZATION_ID)).willReturn(response);

        // WHEN
        FindUsersByOrganisationResponse result = repository.findUsersByOrganisation(USER_AUTH_TOKEN, ORGANIZATION_ID);

        // THEN
        assertThat(result).isSameAs(response);
    }

    @Test
    @DisplayName("Find organisation address")
    void shouldFindOrganisationAddress_forAGivenOrganisationId() {

        // GIVEN
        FindOrganisationResponse response = new FindOrganisationResponse(null, null, null);
        given(apiClient.findOrganisation(ORGANIZATION_ID)).willReturn(response);

        // WHEN
        FindOrganisationResponse result = repository.findOrganisationAddress(ORGANIZATION_ID);

        // THEN
        assertThat(result).isSameAs(response);
    }

}
