package uk.gov.hmcts.reform.managecase.api.errorhandling;

public class UpdateSupplementaryDataException extends RuntimeException {

    public UpdateSupplementaryDataException(String message) {
        super(message);
    }
}
