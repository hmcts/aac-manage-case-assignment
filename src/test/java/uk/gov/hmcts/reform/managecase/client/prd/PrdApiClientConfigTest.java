package uk.gov.hmcts.reform.managecase.client.prd;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PrdApiClientConfigTest {

    @Test
    void shouldCreatePrdClientBeans() {
        PrdApiClientConfig configuration = new PrdApiClientConfig();

        assertThat(configuration.authHeadersInterceptor(mock(SecurityUtils.class))).isNotNull();
        assertThat(configuration.client()).isInstanceOf(PrdFeignClient.class);
    }

    @Test
    void shouldCreateConfiguredRetryer() {
        assertThat(new PrdApiClientConfig().retryer(10, 100, 3)).isInstanceOf(feign.Retryer.Default.class);
    }
}
