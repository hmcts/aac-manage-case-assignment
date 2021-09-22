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
    public static final String CHALLENGE_QUESTION_ANSWERS_EMPTY = "Challenge question answers can not be empty";
    public static final String NOC_REQUEST_ONGOING = "Ongoing NoC request in progress";
    public static final String CHANGE_REQUEST = "More than one change request found on the case";
    public static final String CASE_NOT_FOUND = "Case could not be found";
    public static final String NOC_EVENT_NOT_AVAILABLE = "No NoC events available for this case type";
    public static final String MULTIPLE_NOC_REQUEST_EVENTS = "Multiple NoC Request events found for the user";
    public static final String INSUFFICIENT_PRIVILEGE = "Insufficient privileges for notice of change request";
    public static final String CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID =
        "ChangeOrganisationRequest field could not be found or appears invalid";

    public static final String NOC_DECISION_EVENT_UNIDENTIFIABLE = "NoC Decision event could not be identified";
    public static final String EVENT_TOKEN_NOT_PRESENT = "Event token not present";
    public static final String NO_ORG_POLICY_WITH_ROLE = "No Organisation Policy for one or more of the roles "
        + "available for the notice of change request";

    public static final String INVALID_CASE_ROLE_FIELD = "CaseRole field within ChangeOrganisationRequest "
        + "matched none or more than one OrganisationPolicy on the case";

    public static final String ASSIGNEE_ROLE_ERROR = "Intended assignee has to be a solicitor"
        + " enabled in the jurisdiction of the case.";
    public static final String ASSIGNEE_ORGANISATION_ERROR = "Intended assignee has to be in the same"
        + " organisation as that of the invoker.";
    public static final String UNASSIGNEE_ORGANISATION_ERROR = "Intended user to be unassigned has to be in the same"
        + " organisation as that of the invoker.";
    public static final String ORGANISATION_POLICY_ERROR = "Case ID has to be one for which a case role is"
        + " represented by the invoker's organisation.";
    public static final String JURISDICTION_CANNOT_BE_BLANK = "Jurisdiction cannot be blank.";
    public static final String NO_ORGANISATION_POLICY_ON_CASE_DATA = "No OrganisationPolicy found on the case data.";
    public static final String NO_SOLICITOR_ORGANISATION_RECORDED_IN_ORG_POLICY = "The organisation of the solicitor is"
        + " not recorded in an organisation policy on the case.";
    public static final String NO_ORGANISATION_ID_IN_ANY_ORG_POLICY = "No Organisation id present in any Org policy"
        + " on the case.";
    public static final String ORG_POLICY_CASE_ROLE_NOT_IN_CASE_DEFINITION = "Missing"
        + " OrganisationPolicy.OrgPolicyCaseAssignedRole %s in the case definition.";

    public static final String CASE_DETAILS_REQUIRED = "'case_details' are required.";
    public static final String NOC_REQUEST_NOT_CONSIDERED = "A decision has not yet been made on the "
        + "pending Notice of Change request.";
    public static final String UNKNOWN_NOC_APPROVAL_STATUS = "Unrecognised value for ApprovalStatus.";
    public static final String NO_DATA_PROVIDED = "No case data has been provided.";
    public static final String COR_MISSING_ORGANISATIONS = "Fields of type ChangeOrganisationRequest must include "
        + "both an OrganisationToAdd and OrganisationToRemove field.";
    public static final String COR_MISSING = "No field of type ChangeOrganisationRequest found.";

    public static final String EMPTY_CASE_ID_LIST = "Case ID list is empty";
    public static final String USER_ID_INVALID = "User ID is not valid";
    public static final String ORGANISATION_ID_INVALID = "Organisation ID is not valid";
    public static final String CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION =
        "Client service not authorised to perform operation";
    public static final String EMPTY_CASE_USER_ROLE_LIST = "Case user roles list is empty";
    public static final String OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED =
        "Access to other user's case role assignments not granted";

    public static final String R_A_NOT_FOUND_FOR_CASE_AND_USER =
        "No Role Assignments found for userIds=%s and casesIds=%s when getting from "
            + "Role Assignment Service because of %s";

    public static final String ROLE_ASSIGNMENTS_CLIENT_ERROR =
        "Client error when %s Role Assignments from Role Assignment Service because of %s";
    public static final String ROLE_ASSIGNMENT_SERVICE_ERROR =
        "Problem %s Role Assignments from Role Assignment Service because of %s";

    public static final String AUTHENTICATION_TOKEN_INVALID =
        "Authentication failure due to invalid / expired tokens (IDAM / S2S).";

    public static final String UNAUTHORISED_S2S_SERVICE = "Unauthorised S2S service";

    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private ValidationError() {
    }
}
