package uk.gov.hmcts.reform.managecase.domain;

public enum ApprovalStatus {

    NOT_CONSIDERED("0"),
    APPROVED("1"),
    REJECTED("2");

    String value;

    ApprovalStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
