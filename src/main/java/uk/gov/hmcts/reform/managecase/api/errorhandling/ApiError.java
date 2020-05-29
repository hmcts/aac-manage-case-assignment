package uk.gov.hmcts.reform.managecase.api.errorhandling;

import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class ApiError {

    private HttpStatus status;
    private String message;
    private List<String> errors;

    public ApiError(final HttpStatus status, final String message, final String error) {
        this.status = status;
        this.message = message;
        errors = Arrays.asList(error);
    }
}
