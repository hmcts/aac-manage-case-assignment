package uk.gov.hmcts.reform.managecase.api.errorhandling.noc;

public enum NoCValidationError {
    NOC_REQUEST_ONGOING("Ongoing NoC request in progress","noc-in-progress"),
    NO_ORG_POLICY_WITH_ROLE("No Organisation Policy for one or more of the roles available for the "
                                + "notice of change request", "no-org-policy"),
    NOC_EVENT_NOT_AVAILABLE("No NoC events available for this case type",
                            "noc-event-unavailable"),
    MULTIPLE_NOC_REQUEST_EVENTS("Multiple NoC Request events found for the user",
                                "multiple-noc-requests-on-user"),
    INSUFFICIENT_PRIVILEGE("Insufficient privileges for notice of change request",
                           "insufficient-privileges"),
    CHANGE_REQUEST("More than one change request found on the case",
                   "multiple-noc-requests-on-case"),
    CASE_ID_INVALID("Case ID has to be a valid 16-digit Luhn number",
                    "case-id-invalid"),
    CASE_ID_EMPTY("Case ID can not be empty", "case-id-empty"),
    CHALLENGE_QUESTION_ANSWERS_EMPTY("Challenge question answers can not be empty",
                                     "answers-empty"),
    INVALID_CASE_ROLE_FIELD("CaseRole field within ChangeOrganisationRequest "
        + "matched none or more than one OrganisationPolicy on the case", "invalid-case-role"),
    CASE_ID_INVALID_LENGTH("Case ID has to be 16-digits long", "case-id-invalid-length");

    public static final String NOC_CASE_ID_INVALID = "Noc Case ID has to be a valid 16-digit Luhn number";
    public static final String NOC_CASE_ID_INVALID_LENGTH = "Noc Case ID has to be 16-digits long";
    public static final String NOC_CASE_ID_EMPTY = "Noc Case ID can not be empty";
    public static final String NOC_CHALLENGE_QUESTION_ANSWERS_EMPTY = "Noc Challenge question answers can not be empty";

    private final String errorMessage;
    private final String errorCode;

    NoCValidationError(String errorMessage, String errorCode) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static String getCodeFromMessage(String message) {
        for (NoCValidationError e : NoCValidationError.values()) {
            if (e.getErrorMessage().equalsIgnoreCase(message)) {
                return e.getErrorCode();
            }
        }
        return null;
    }

}
