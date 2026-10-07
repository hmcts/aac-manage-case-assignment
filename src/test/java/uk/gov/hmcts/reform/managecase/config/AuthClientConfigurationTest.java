package uk.gov.hmcts.reform.managecase.config;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthClientConfigurationTest {

    @Test
    void shouldCreateAuthTokenGenerator() {
        AuthTokenGenerator generator = new AuthClientConfiguration().authTokenGenerator(
            "AAAAAAAAAAAAAAAA",
            "aac_manage_case_assignment",
            mock(ServiceAuthorisationApi.class));

        assertThat(generator).isNotNull();
    }
}
