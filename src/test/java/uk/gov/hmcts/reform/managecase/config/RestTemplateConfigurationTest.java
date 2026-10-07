package uk.gov.hmcts.reform.managecase.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RestTemplateConfigurationTest {

    @Test
    void shouldCreateRestTemplateWithConfiguredHttpClientFactory() {
        RestTemplateConfiguration configuration = new RestTemplateConfiguration();
        ReflectionTestUtils.setField(configuration, "maxTotalHttpClient", 10);
        ReflectionTestUtils.setField(configuration, "maxSecondsIdleConnection", 30);
        ReflectionTestUtils.setField(configuration, "maxClientPerRoute", 5);
        ReflectionTestUtils.setField(configuration, "validateAfterInactivity", 10);
        ReflectionTestUtils.setField(configuration, "connectionTimeout", 10);
        ReflectionTestUtils.setField(configuration, "readTimeout", 10);

        RestTemplate restTemplate = configuration.restTemplate();

        assertThat(restTemplate.getRequestFactory()).isInstanceOf(
            org.springframework.http.client.HttpComponentsClientHttpRequestFactory.class);
    }
}
