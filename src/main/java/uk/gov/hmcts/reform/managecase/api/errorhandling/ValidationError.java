package uk.gov.hmcts.reform.managecase.api.errorhandling;

public final class ValidationError {

    public static final String ASSIGNEE_ID_EMPTY = "IDAM Assignee ID can not be empty";
    public static final String CASE_ID_INVALID = "Case ID has to be a valid 16-digit Luhn number";
    public static final String CASE_ID_EMPTY = "Case ID can not be empty";
    public static final String CASE_ROLE_FORMAT_INVALID = "Case role name format is invalid";
    public static final String EMPTY_REQUESTED_UNASSIGNMENTS_LIST = "Requested Unassignments can not be empty";
    public static final String INVOKER_NOT_IN_SAME_ORGANISATION_AS_UNASSIGNED_USER
        = "Intended user to be unassigned has to be in the same organisation as that of the invoker";

    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private ValidationError() {
    }
}
