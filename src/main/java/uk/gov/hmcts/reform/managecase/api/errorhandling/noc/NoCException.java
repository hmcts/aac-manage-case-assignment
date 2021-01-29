package uk.gov.hmcts.reform.managecase.api.errorhandling.noc;

@SuppressWarnings({"PMD.MissingSerialVersionUID"})
public class NoCException extends RuntimeException {
    public String errorCode;
    public String errorMessage;

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
}
