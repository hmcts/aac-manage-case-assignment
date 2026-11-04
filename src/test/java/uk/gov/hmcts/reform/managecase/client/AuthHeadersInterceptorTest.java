package uk.gov.hmcts.reform.managecase.client;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

class AuthHeadersInterceptorTest {

    @Test
    void shouldAddMissingUserAndServiceHeaders() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getUserBearerToken()).thenReturn("user-token");
        when(securityUtils.getS2SToken()).thenReturn("s2s-token");
        RequestTemplate template = new RequestTemplate();

        new AuthHeadersInterceptor(securityUtils).apply(template);

        assertThat(template.headers().get(AUTHORIZATION)).containsExactly("user-token");
        assertThat(template.headers().get(SERVICE_AUTHORIZATION)).containsExactly("s2s-token");
    }

    @Test
    void shouldPreserveExistingHeaders() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        RequestTemplate template = new RequestTemplate();
        template.header(AUTHORIZATION, "existing-user");
        template.header(SERVICE_AUTHORIZATION, "existing-s2s");

        new AuthHeadersInterceptor(securityUtils).apply(template);

        assertThat(template.headers().get(AUTHORIZATION)).containsExactly("existing-user");
        assertThat(template.headers().get(SERVICE_AUTHORIZATION)).containsExactly("existing-s2s");
    }
}
