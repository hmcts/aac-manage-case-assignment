package uk.gov.hmcts.reform.managecase.api.errorhandling;

public class CaseCouldNotBeFetchedException extends RuntimeException {

    public CaseCouldNotBeFetchedException(String message) {
        super(message);
    }
}
