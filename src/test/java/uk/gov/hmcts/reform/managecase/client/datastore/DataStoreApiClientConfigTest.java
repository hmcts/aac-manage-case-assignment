package uk.gov.hmcts.reform.managecase.client.datastore;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DataStoreApiClientConfigTest {

    @Test
    void shouldCreateSystemUserInterceptor() {
        assertThat(new DataStoreApiClientConfig().systemUserAuthHeadersInterceptor(mock(SecurityUtils.class)))
            .isNotNull();
    }

    @Test
    void shouldDisableRetries() {
        assertThat(new DataStoreApiClientConfig().retryer()).isSameAs(feign.Retryer.NEVER_RETRY);
    }
}
