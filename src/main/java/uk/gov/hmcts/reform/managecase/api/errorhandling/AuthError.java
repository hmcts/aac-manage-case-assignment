package uk.gov.hmcts.reform.managecase.api.errorhandling;

/**
 * Authentication and Authorisation error messages.
 **/
public final class AuthError {

    public static final String AUTHENTICATION_TOKEN_INVALID
        = "Authentication failure due to invalid / expired tokens (IDAM / S2S).";
    public static final String UNAUTHORISED_S2S_SERVICE = "Unauthorised S2S service.";

    public static final String OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED =
        "Access to other user's case role assignments not granted";

    // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    private AuthError() {
    }
}
