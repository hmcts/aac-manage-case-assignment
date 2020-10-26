package uk.gov.hmcts.reform.managecase.client.datastore;

public enum ApprovalStatus {

    NOT_CONSIDERED("0"),
    APPROVED("1"),
    REJECTED("2");

    private String code;

    ApprovalStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
