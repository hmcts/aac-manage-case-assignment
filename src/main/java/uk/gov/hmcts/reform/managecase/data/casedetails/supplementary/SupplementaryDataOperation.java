package uk.gov.hmcts.reform.managecase.data.casedetails.supplementary;

public enum SupplementaryDataOperation {
    INC("$inc"),
    SET("$set"),
    FIND("$find");

    private String operationName;

    SupplementaryDataOperation(String opName) {
        this.operationName = opName;
    }

    public String getOperationName() {
        return this.operationName;
    }

}
