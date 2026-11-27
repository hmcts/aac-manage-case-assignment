package uk.gov.hmcts.reform.managecase.api.errorhandling;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** Keeps authentication diagnostics in server logs, not in the Bearer challenge. */
@Slf4j
public class SafeAuthenticationErrors
    implements AuthenticationEntryPoint, AccessDeniedHandler, AuthenticationFailureHandler {

    public static final String ACCESS_DENIED_CHALLENGE = "Bearer error=\"insufficient_scope\"";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            if (oauthException.getError() instanceof BearerTokenError bearerError) {
                status = bearerError.getHttpStatus();
            }
        }
        try (var diagnostics = FailureDiagnostics.open(exception, status.value())) {
            log.warn("Authentication failed: status={}", status.value(), exception);
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, authenticationChallenge(exception));
        response.setStatus(status.value());
    }

    public static String authenticationChallenge(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            String error = oauthException.getError().getErrorCode();
            if (java.util.Set.of("invalid_request", "invalid_token", "insufficient_scope").contains(error)) {
                return "Bearer error=\"" + error + "\"";
            }
        }
        return "Bearer";
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) {
        commence(request, response, exception);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception) {
        try (var diagnostics = FailureDiagnostics.open(exception, 403)) {
            log.warn("Access denied: status=403", exception);
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
            request.getUserPrincipal()
                instanceof AbstractOAuth2TokenAuthenticationToken<?>
                ? ACCESS_DENIED_CHALLENGE : "Bearer");
        response.setStatus(HttpStatus.FORBIDDEN.value());
    }
}
