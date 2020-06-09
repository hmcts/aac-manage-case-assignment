package uk.gov.hmcts.reform.managecase.api.errorhandling;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.validation.ValidationException;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String[] errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getDefaultMessage())
            .toArray(String[]::new);
        return toResponseEntity(status, ex.getLocalizedMessage(), errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(Exception ex) {
        return toResponseEntity(HttpStatus.FORBIDDEN, ex.getLocalizedMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidationException(Exception ex) {
        return toResponseEntity(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(final Exception ex) {
        log.error(ex.getMessage(), ex);
        return toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage());
    }

    private ResponseEntity<Object> toResponseEntity(HttpStatus status, String errorMessage, String... errors) {
        ApiError apiError = new ApiError(status, errorMessage, errors == null ? null : Lists.newArrayList(errors));
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }
}
