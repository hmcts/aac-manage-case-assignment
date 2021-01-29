package uk.gov.hmcts.reform.managecase.api.errorhandling.noc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class NoCApiError {

    private HttpStatus status;
    private String message;
    private String code;
    private List<String> errors;

}
