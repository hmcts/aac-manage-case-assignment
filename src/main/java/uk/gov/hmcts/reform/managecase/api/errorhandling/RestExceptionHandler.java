package uk.gov.hmcts.reform.managecase.api.errorhandling;

import com.google.common.collect.ImmutableList;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCApiError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import java.util.Arrays;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final List<String> NOC_CONSTRAINT_ERRORS = ImmutableList.of(
        NoCValidationError.NOC_CASE_ID_EMPTY,
        NoCValidationError.NOC_CASE_ID_INVALID,
        NoCValidationError.NOC_CASE_ID_INVALID_LENGTH,
        NoCValidationError.NOC_CHALLENGE_QUESTION_ANSWERS_EMPTY
    );

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        logFailure(ex, status.value());
        String[] errors = ex.getBindingResult().getFieldErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .toArray(String[]::new);
        return toResponseEntity(HttpStatus.valueOf(status.value()), HttpStatus.BAD_REQUEST.getReasonPhrase(), errors);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(ex, null, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
                                                              HttpHeaders headers, HttpStatusCode status,
                                                              WebRequest request) {
        logFailure(ex, status.value());
        HttpStatus known = HttpStatus.resolve(status.value());
        var problem = org.springframework.http.ProblemDetail.forStatusAndDetail(status,
            known == null ? "Request failed" : known.getReasonPhrase());
        // Preserve Spring's pre-existing ProblemDetail fields, without rejected values or parser diagnostics.
        problem.setInstance(java.net.URI.create("/"));
        return super.handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler({CaseAssignedUserRoleException.class})
    @ResponseBody
    public ResponseEntity<Object> handleApiException(final CaseAssignedUserRoleException ex) {

        var responseStatus = ex.getClass().getAnnotation(ResponseStatus.class);
        HttpStatus status = getHttpStatus(responseStatus);
        logFailure(ex, status.value());
        return toResponseEntity(status, ex.getCause() == null ? ex.getLocalizedMessage() : status.getReasonPhrase());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex) {
        logFailure(ex, 400);
        String[] errors = ex.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .toArray(String[]::new);
        return toResponseEntity(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex) {
        logFailure(ex, 403);
        return toResponseEntity(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.getReasonPhrase());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidationException(ValidationException ex) {
        logFailure(ex, 400);
        return toResponseEntity(HttpStatus.BAD_REQUEST, ex.getClass() == ValidationException.class
            ? ex.getLocalizedMessage() : HttpStatus.BAD_REQUEST.getReasonPhrase());
    }

    @ExceptionHandler(CaseCouldNotBeFoundException.class)
    public ResponseEntity<Object> handleCaseCouldNotBeFoundException(CaseCouldNotBeFoundException ex) {
        logFailure(ex, 404);
        return toResponseEntity(HttpStatus.NOT_FOUND, ex.getLocalizedMessage());
    }

    @ExceptionHandler(NoCException.class)
    public ResponseEntity<Object> handleNoCException(NoCException ex) {
        logFailure(ex, 400);
        return toNoCResponseEntity(ex.getErrorMessage(), ex.getErrorCode());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Object> handleFeignStatusException(FeignException ex) {
        logFailure(ex, 502);
        return toResponseEntity(HttpStatus.BAD_GATEWAY, HttpStatus.BAD_GATEWAY.getReasonPhrase());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(final Exception ex) {
        logFailure(ex, 500);
        return toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    }

    private String[] convertNoCErrors(String[] errors) {
        List<String> errorList = Arrays.asList(errors);
        for (String error : errorList) {
            for (String exception : NOC_CONSTRAINT_ERRORS) {
                if (error.equals(exception)) {
                    errorList.set(errorList.indexOf(exception), exception.substring(4));
                    break;
                }
            }
        }
        return errorList.toArray(new String[0]);
    }

    private ResponseEntity<Object> handleConstraintErrors(String[] errorsToConvert, HttpStatus status) {
        List<String> errorList = Arrays.asList(errorsToConvert);
        String error = errorList.get(0);
        for (String exception : NOC_CONSTRAINT_ERRORS) {
            if (error.equals(exception)) {
                String message = exception.substring(4);
                String code = NoCValidationError.getCodeFromMessage(message);
                String[] errors = convertNoCErrors(errorsToConvert);
                NoCApiError apiError = new NoCApiError(status, message, code, List.of(errors));
                return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
            }
        }
        return null;
    }


    private ResponseEntity<Object> toResponseEntity(HttpStatus status, String message, String... errors) {
        if (errors != null && Arrays.stream(errors).anyMatch(NOC_CONSTRAINT_ERRORS::contains)) {
            return handleConstraintErrors(errors, status);
        }
        ApiError apiError = new ApiError(status, message, errors == null ? null : List.of(errors));
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());

    }

    private ResponseEntity<Object> toNoCResponseEntity(String message, String code,
                                                       String... errors) {
        NoCApiError apiError = new NoCApiError(HttpStatus.BAD_REQUEST, message, code,
                                               errors == null ? null : List.of(errors));
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    private HttpStatus getHttpStatus(ResponseStatus responseStatus) {
        if (!HttpStatus.INTERNAL_SERVER_ERROR.equals(responseStatus.value())) {
            return responseStatus.value();
        } else if (!HttpStatus.INTERNAL_SERVER_ERROR.equals(responseStatus.code())) {
            return responseStatus.code();
        }

        return null;
    }

    private void logFailure(Exception failure, int status) {
        String businessCode = failure instanceof NoCException noc ? noc.getErrorCode() : null;
        try (var diagnostics = FailureDiagnostics.open(failure, status, businessCode)) {
            if (status >= 500) {
                log.error("Request failed: status={}", status, failure);
            } else {
                log.warn("Request failed: status={}", status, failure);
            }
        }
    }
}
