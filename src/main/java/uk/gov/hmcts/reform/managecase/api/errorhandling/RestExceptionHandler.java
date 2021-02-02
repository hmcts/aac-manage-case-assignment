package uk.gov.hmcts.reform.managecase.api.errorhandling;

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
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCApiError;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

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

    @ExceptionHandler(CaseCouldNotBeFetchedException.class)
    public ResponseEntity<Object> handleCaseCouldNotBeFetchedException(CaseCouldNotBeFetchedException ex) {
        log.error("Data Store errors: {}", ex.getMessage(), ex);
        return toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, ex.getLocalizedMessage());
    }

    @ExceptionHandler(CaseCouldNotBeFoundException.class)
    public ResponseEntity<Object> handleCaseCouldNotBeFoundException(CaseCouldNotBeFoundException ex) {
        log.error("Data Store errors: {}", ex.getMessage(), ex);
        return toResponseEntity(HttpStatus.NOT_FOUND, ex.getLocalizedMessage());
    }

    @ExceptionHandler(NoCException.class)
    public ResponseEntity<Object> handleNoCException(NoCException ex) {
        log.debug("NoC Validation exception: {}", ex.getMessage(), ex);
        return toNoCResponseEntity(HttpStatus.BAD_REQUEST, ex.errorMessage, ex.errorCode);
    }

    @ExceptionHandler(CaseIdLuhnException.class)
    public ResponseEntity<Object> handleCaseIdLuhnException(CaseIdLuhnException ex) {
        log.error("Data Store errors: {}", ex.getMessage(), ex);
        return toResponseEntity(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage());
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

    private List<String> noCExceptions = new ArrayList<>(Arrays.asList(
        NoCValidationError.NOC_CASE_ID_EMPTY,
        NoCValidationError.NOC_CASE_ID_INVALID,
        NoCValidationError.NOC_CASE_ID_INVALID_LENGTH,
        NoCValidationError.NOC_CHALLENGE_QUESTION_ANSWERS_EMPTY
    ));

    private String[] hello(String[] errors) {
        List<String> er = Arrays.asList(errors);
        for (String ex : errors) {
            for (String ux : noCExceptions) {
                if (ex.equals(ux)) {
                    String message = ux.substring(4);
                    int hi = er.indexOf(ux);
                    er.set(hi, message);
                }
            }
        }
        return er.toArray(new String[0]);
    }


    private ResponseEntity<Object> toResponseEntity(HttpStatus status, String errorMessage, String... errors) {
        String[] hey = hello(errors);
        for (String ex : errors) {
            for (String ux : noCExceptions) {
                if (ex.equals(ux)) {
                    String message = ux.substring(4);
                    String errorCode = NoCValidationError.getCodeFromMessage(message);
                    NoCApiError apiError = new NoCApiError(status, message, errorCode, List.of(hey)
                    );
                    return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
                }
            }
        }
        ApiError apiError = new ApiError(status, errorMessage, errors == null ? null : List.of(errors));
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }

    private ResponseEntity<Object> toNoCResponseEntity(HttpStatus status, String errorMessage, String errorCode,
                                                       String... errors) {
        NoCApiError apiError = new NoCApiError(status, errorMessage, errorCode, errors == null ? null : List.of(errors)
        );
        return new ResponseEntity<>(apiError, new HttpHeaders(), apiError.getStatus());
    }
}
