package uk.gov.hmcts.reform.managecase.config;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.ApplicationParams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotifyClientConfigurationTest {

    @Test
    void shouldCreateNotificationClientWithConfiguredKey() {
        ApplicationParams applicationParams = mock(ApplicationParams.class);
        when(applicationParams.getNotifyApiKey()).thenReturn("notify-api-key");

        assertThat(new NotifyClientConfiguration().notificationClient(applicationParams)).isNotNull();
    }
}
