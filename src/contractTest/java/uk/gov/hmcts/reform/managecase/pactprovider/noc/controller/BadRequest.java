package uk.gov.hmcts.reform.managecase.pactprovider.noc.controller;

public class BadRequest {
    private final String status;
    private final String message;
    private final String code;
    private final String[] errors;

    public BadRequest(final String status, final String message, final String code, final String[] errors) {
        this.status = status;
        this.message = message;
        this.code = code;
        this.errors = errors;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public String[] getErrors() {
        return errors;
    }
}
