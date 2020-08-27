package uk.gov.hmcts.reform.managecase.api.errorhandling;

/**
 * Validation error messages.
 **/
public final class ValidationError {

    public static final String ASSIGNEE_ID_EMPTY = "IDAM Assignee ID can not be empty";
    public static final String CASE_ID_EMPTY = "Case ID can not be empty";
    public static final String CASE_ID_INVALID = "Case ID has to be a valid 16-digit Luhn number";
    public static final String CASE_ID_INVALID_LENGTH = "Case ID has to be 16-digits long";
    public static final String CASE_ROLE_FORMAT_INVALID = "Case role name format is invalid";
    public static final String CASE_TYPE_ID_EMPTY = "Case type ID can not be empty";
    public static final String EMPTY_REQUESTED_UNASSIGNMENTS_LIST = "Requested Unassignments can not be empty";

    public static final String ASSIGNEE_ROLE_ERROR = "Intended assignee has to be a solicitor"
        + " enabled in the jurisdiction of the case";
    public static final String ASSIGNEE_ORGANISATION_ERROR = "Intended assignee has to be in the same"
        + " organisation as that of the invoker";
    public static final String UNASSIGNEE_ORGANISATION_ERROR = "Intended user to be unassigned has to be in the same"
        + " organisation as that of the invoker";
    public static final String ORGANISATION_POLICY_ERROR = "Case ID has to be one for which a case role is"
        + " represented by the invoker's organisation";

    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private ValidationError() {
    }
}
