package uk.gov.hmcts.reform.managecase.api.errorhandling;

public final class ValidationError {

    public static final String CASE_ID_INVALID = "Case ID has to be a valid 16-digit Luhn number";
    public static final String CASE_ID_EMPTY = "Case ID can not be empty";

    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private ValidationError() {
    }
}
