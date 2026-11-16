package uk.gov.hmcts.reform.managecase.api.errorhandling;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCException;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void shouldMapAccessDeniedToForbidden() {
        var response = handler.handleAccessDeniedException(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(((ApiError) response.getBody()).getMessage()).isEqualTo("Forbidden");
    }

    @Test
    void shouldMapValidationToBadRequest() {
        var response = handler.handleValidationException(new jakarta.validation.ValidationException("invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((ApiError) response.getBody()).getMessage()).isEqualTo("invalid");
    }

    @Test
    void shouldMapMissingCaseToNotFound() {
        var response = handler.handleCaseCouldNotBeFoundException(
            new CaseCouldNotBeFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(((ApiError) response.getBody()).getMessage()).isEqualTo("missing");
    }

    @Test
    void shouldMapCaseAssignmentErrorUsingResponseStatus() {
        var response = handler.handleApiException(new CaseAssignedUserRoleException("invalid role"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(((ApiError) response.getBody()).getMessage()).isEqualTo("invalid role");
        assertThat(CaseAssignedUserRoleException.class.getAnnotation(ResponseStatus.class).code())
            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void shouldMapNoCErrorToStructuredBadRequest() {
        var response = handler.handleNoCException(new NoCException("invalid answers", "answers-invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        var body = (uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCApiError) response.getBody();
        assertThat(body.getMessage()).isEqualTo("invalid answers");
        assertThat(body.getCode()).isEqualTo("answers-invalid");
    }

    @Test
    void shouldHideWrappedDependencyDiagnosticsAndRetainOriginalLogs() {
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(RestExceptionHandler.class);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        var cause = new IllegalStateException("SECRET upstream SQL and URL");
        var exception = new BadRequestException("SECRET downstream response", cause);
        try {
            var response = handler.handleApiException(exception);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(((ApiError) response.getBody()).getMessage()).isEqualTo("Bad Request");
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getLevel()).isEqualTo(ch.qos.logback.classic.Level.WARN);
            assertThat(appender.list.get(0).getThrowableProxy().getMessage())
                .isEqualTo(exception.getMessage());
            assertThat(appender.list.get(0).getThrowableProxy().getCause().getMessage()).isEqualTo(cause.getMessage());
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void shouldHideMalformedRequestDiagnostics() {
        var request = new org.springframework.mock.http.MockHttpInputMessage(new byte[0]);
        var exception = new org.springframework.http.converter.HttpMessageNotReadableException(
            "SECRET internal class and parser details", request);
        var response = handler.handleHttpMessageNotReadable(exception, new org.springframework.http.HttpHeaders(),
            HttpStatus.BAD_REQUEST, new org.springframework.web.context.request.ServletWebRequest(
                new org.springframework.mock.web.MockHttpServletRequest()));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((org.springframework.http.ProblemDetail) response.getBody()).getDetail()).isEqualTo("Bad Request");
    }

    @Test
    void shouldHideDownstreamResponseBody() {
        var request = feign.Request.create(feign.Request.HttpMethod.GET, "http://private-service",
            java.util.Map.of(), null, java.nio.charset.StandardCharsets.UTF_8, null);
        var downstream = feign.Response.builder().request(request).status(500)
            .headers(java.util.Map.of()).body("SECRET SQL diagnostic", java.nio.charset.StandardCharsets.UTF_8).build();
        var response = handler.handleFeignStatusException(feign.FeignException.errorStatus("fetch", downstream));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(((ApiError) response.getBody()).getMessage()).isEqualTo("Bad Gateway");
    }

    @Test
    void shouldMapUnexpectedExceptionToInternalServerError() {
        var response = handler.handleAll(new IllegalStateException("unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(((ApiError) response.getBody()).getMessage()).isEqualTo("Internal Server Error");
    }
}
