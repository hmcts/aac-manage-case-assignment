package uk.gov.hmcts.reform.managecase.api.errorhandling.noc;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NoCApiConstraintError {


    private HttpStatus status;
    private String code;
    private List<String> errors;


}
