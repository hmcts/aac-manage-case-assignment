package uk.gov.hmcts.reform.managecase.api.errorhandling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenError;

import static org.assertj.core.api.Assertions.assertThat;

class SafeAuthenticationErrorsTest {
    @ParameterizedTest
    @ValueSource(ints = {400, 401})
    void retainsProtocolStatusWithoutDecoderDiagnostics(int status) throws Exception {
        String code = status == 400 ? "invalid_request" : "invalid_token";
        var error = new BearerTokenError(code, HttpStatus.valueOf(status),
            "PRIVATE decoder details", "http://private-service/jwks");
        var response = new MockHttpServletResponse();
        new SafeAuthenticationErrors().onAuthenticationFailure(new MockHttpServletRequest(), response,
            new OAuth2AuthenticationException(error));
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer error=\"" + code + "\"");
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void retainsAccessDeniedStatusAndAnonymousChallenge() {
        var response = new MockHttpServletResponse();
        new SafeAuthenticationErrors().handle(new MockHttpServletRequest(), response,
            new AccessDeniedException("PRIVATE diagnostic"));
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }
}
