package uk.gov.hmcts.reform.managecase.client.definitionstore;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DefinitionStoreApiClientConfigTest {

    @Test
    void shouldCreateSystemUserInterceptor() {
        assertThat(new DefinitionStoreApiClientConfig()
            .systemUserAuthHeadersInterceptor(mock(SecurityUtils.class))).isNotNull();
    }

    @Test
    void shouldDisableRetries() {
        assertThat(new DefinitionStoreApiClientConfig().retryer())
            .isSameAs(feign.Retryer.NEVER_RETRY);
    }
}
