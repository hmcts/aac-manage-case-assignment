package uk.gov.hmcts.reform.managecase.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.PrdApiClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.openMocks;

class PrdRepositoryTest {

    @Mock
    private PrdApiClient apiClient;

    @InjectMocks
    private DefaultPrdRepository repository;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    @DisplayName("Find users in the invokers organisation")
    void shouldFindUsersByOrganisation() {

        FindUsersByOrganisationResponse response = new FindUsersByOrganisationResponse(null, null);

        given(apiClient.findActiveUsersByOrganisation()).willReturn(response);

        FindUsersByOrganisationResponse result = repository.findUsersByOrganisation();

        assertThat(result).isSameAs(response);
    }

}