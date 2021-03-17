package uk.gov.hmcts.reform.managecase.api.errorhandling;

@SuppressWarnings({"PMD.MissingSerialVersionUID"})
public class CaseCouldNotBeFoundException extends RuntimeException {

    public CaseCouldNotBeFoundException(String message) {
        super(message);
    }
}
