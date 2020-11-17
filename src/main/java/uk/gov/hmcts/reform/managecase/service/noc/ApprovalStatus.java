package uk.gov.hmcts.reform.managecase.service.noc;

public enum ApprovalStatus {
    APPROVED("1"), PENDING("0");

    String value;

    ApprovalStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}