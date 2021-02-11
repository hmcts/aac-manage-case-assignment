package uk.gov.hmcts.reform.managecase.api.errorhandling;

import com.google.common.collect.ImmutableList;
import feign.FeignException;
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
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCApiConstraintError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCApiError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError;

import javax.validation.ValidationException;
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
        MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        String[] errors = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getDefaultMessage())
            .toArray(String[]::new);
        log.debug("MethodArgumentNotValidException:{}", ex.getLocalizedMessage());
        return toResponseEntity(status, null, errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(Exception ex) {
        log.warn("Access denied due to:", ex);
        return toResponseEntity(HttpStatus.FORBIDDEN, ex.getLocalizedMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> handleValidationException(Exception ex) {
        log.debug("Validation exception:", ex);
        return toResponseEntity(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
    }

    @ExceptionHandler(CaseCouldNotBeFoundException.class)
    public ResponseEntity<Object> handleCaseCouldNotBeFoundException(CaseCouldNotBeFoundException ex) {
        log.error("Data Store errors: {}", ex.getMessage(), ex);
        return toResponseEntity(HttpStatus.NOT_FOUND, ex.getLocalizedMessage());
    }

    @ExceptionHandler(NoCException.class)
    public ResponseEntity<Object> handleNoCException(NoCException ex) {
        log.debug("NoC Validation exception: {}", ex.getMessage(), ex);
        return toNoCResponseEntity(HttpStatus.BAD_REQUEST, ex.getErrorMessage(), ex.getErrorCode());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Object> handleFeignStatusException(FeignException ex) {
        log.error("Downstream service errors: {}", ex.getMessage(), ex);
        return toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage());
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
                if ((errorsToConvert.length > 1)) {
                    NoCApiError apiError = new NoCApiError(status, message, code, List.of(errors));
                    return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
                } else {
                    NoCApiConstraintError apiError = new NoCApiConstraintError(status, code, List.of(errors)
                    );
                    return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
                }
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

    private ResponseEntity<Object> toNoCResponseEntity(HttpStatus status, String message, String code,
                                                       String... errors) {
        NoCApiError apiError = new NoCApiError(status, message, code, errors == null ? null : List.of(errors)
        );
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }
}
