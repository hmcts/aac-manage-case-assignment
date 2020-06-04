package uk.gov.hmcts.reform.managecase.api.controller;

@SuppressWarnings("PMD.ShortClassName")
public final class V1 {
    private V1() {
    }

    public final class MediaType {

        public static final String CASE_ASSIGNMENT_RESPONSE = "application/vnd.uk.gov.hmcts.ccd.manage-case-assignment"
            + ".assign-access-within-organisation-response-payload.v1+json;charset=UTF-8";

        private MediaType() {
        }
    }

    public final class Error {

        public static final String CASE_NOT_FOUND = "Case not found";

        private Error() {
        }
    }
}
