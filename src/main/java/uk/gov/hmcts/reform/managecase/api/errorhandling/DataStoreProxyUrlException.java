package uk.gov.hmcts.reform.managecase.api.errorhandling;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DataStoreProxyUrlException extends RuntimeException {

    private static final long serialVersionUID = -752785721547590446L;

    public DataStoreProxyUrlException(String message) {
        super(message);
    }

}
