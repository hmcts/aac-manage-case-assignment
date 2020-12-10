package uk.gov.hmcts.reform.managecase.api.errorhandling;

@SuppressWarnings({"PMD.MissingSerialVersionUID"})
public class CaseIdLuhnException extends RuntimeException {

    public CaseIdLuhnException(String message) {
        super(message);
    }
}
