package uk.gov.hmcts.reform.managecase.client.datastore.model;

import uk.gov.hmcts.reform.managecase.client.datastore.model.AccessControlList;

public class ComplexACL extends AccessControlList {
    private String listElementCode;

    public String getListElementCode() {
        return listElementCode;
    }

    public void setListElementCode(String listElementCode) {
        this.listElementCode = listElementCode;
    }
}
