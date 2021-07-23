package uk.gov.hmcts.reform.managecase.api.errorhandling;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN)
public class CaseRoleAccessException extends CaseAssignedUserRoleException {

    public CaseRoleAccessException(String message) {
        super(message);
    }
}
