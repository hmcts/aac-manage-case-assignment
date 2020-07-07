package uk.gov.hmcts.reform.managecase.api.errorhandling;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class DataStoreProxyUrlException extends RuntimeException {

    public DataStoreProxyUrlException(String message) {
        super(message);
    }

}
