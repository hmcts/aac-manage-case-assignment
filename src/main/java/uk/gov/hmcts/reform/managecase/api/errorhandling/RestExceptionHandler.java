package uk.gov.hmcts.reform.managecase.api.errorhandling;

import com.google.common.collect.ImmutableList;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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
import java.nio.charset.StandardCharsets;
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
        log.error("Method argument not valid: {}", ex.getLocalizedMessage());
        String[] errors = ex.getBindingResult().getFieldErrors().stream()
            .map(DefaultMessageSourceResolvable::getDefaultMessage)
            .toArray(String[]::new);
        return toResponseEntity(HttpStatus.valueOf(status.value()), null, errors);
    }

    @ExceptionHandler({CaseAssignedUserRoleException.class})
    @ResponseBody
    public ResponseEntity<Object> handleApiException(final CaseAssignedUserRoleException ex) {
        log.error("Case assigned user roles: {}", ex.getLocalizedMessage());
        var responseStatus = ex.getClass().getAnnotation(ResponseStatus.class);
        return toResponseEntity(getHttpStatus(responseStatus), ex.getLocalizedMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex) {
        log.error("Constraint violation: {}", ex.getLocalizedMessage());
        String[] errors = ex.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .toArray(String[]::new);
        return toResponseEntity(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access denied due to: {}", ex.getLocalizedMessage());
        return toResponseEntity(HttpStatus.FORBIDDEN, ex.getLocalizedMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidationException(ValidationException ex) {
        log.error("Validation exception: {}", ex.getLocalizedMessage());
        return toResponseEntity(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
    }

    @ExceptionHandler(CaseCouldNotBeFoundException.class)
    public ResponseEntity<Object> handleCaseCouldNotBeFoundException(CaseCouldNotBeFoundException ex) {
        log.error("Case could not be found: {}", ex.getLocalizedMessage());
        return toResponseEntity(HttpStatus.NOT_FOUND, ex.getLocalizedMessage());
    }

    @ExceptionHandler(NoCException.class)
    public ResponseEntity<Object> handleNoCException(NoCException ex) {
        log.error("NoC Validation:", ex);
        return toNoCResponseEntity(ex.getErrorMessage(), ex.getErrorCode());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Object> handleFeignStatusException(FeignException ex) {
        String errorMessage = ex.responseBody()
            .map(res -> new String(res.array(), StandardCharsets.UTF_8))
            .orElse(ex.getMessage());
        log.error("Downstream service errors: {}", errorMessage);
        return toResponseEntity(HttpStatus.BAD_GATEWAY, errorMessage);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(final Exception ex) {
        log.error(ex.getMessage(), ex);
        return toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage());
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
}
