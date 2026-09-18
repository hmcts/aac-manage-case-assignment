package uk.gov.hmcts.reform.managecase.api.errorhandling;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void shouldMapAccessDeniedToForbidden() {
        var response = handler.handleAccessDeniedException(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(((ApiError) response.getBody()).getMessage()).isEqualTo("denied");
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
}
