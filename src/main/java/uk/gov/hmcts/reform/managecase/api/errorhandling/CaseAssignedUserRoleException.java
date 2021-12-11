package uk.gov.hmcts.reform.managecase.api.errorhandling;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.UNPROCESSABLE_ENTITY)
public class CaseAssignedUserRoleException extends RuntimeException {

    public CaseAssignedUserRoleException(String message) {
        super(message);
    }

    public CaseAssignedUserRoleException(final String message, final Throwable e) {
        super(message, e);
    }

}
