package uk.gov.hmcts.reform.managecase.api.errorhandling;

@SuppressWarnings({"PMD.MissingSerialVersionUID"})
public class CaseCouldNotBeFetchedException extends RuntimeException {

    public CaseCouldNotBeFetchedException(String message) {
        super(message);
    }
}
