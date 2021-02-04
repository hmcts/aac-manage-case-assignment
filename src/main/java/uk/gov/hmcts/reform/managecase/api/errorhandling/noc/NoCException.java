package uk.gov.hmcts.reform.managecase.api.errorhandling.noc;

@SuppressWarnings({"PMD.MissingSerialVersionUID"})
public class NoCException extends RuntimeException {
    private final String errorCode;
    private final String errorMessage;

    public NoCException(NoCValidationError message) {
        super(message.getErrorMessage());
        errorCode = message.getErrorCode();
        errorMessage = message.getErrorMessage();
    }

    public NoCException(String message, String code) {
        super(message);
        errorCode = code;
        errorMessage = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
