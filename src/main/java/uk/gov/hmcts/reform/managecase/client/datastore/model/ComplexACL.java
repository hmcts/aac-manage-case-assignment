package uk.gov.hmcts.reform.managecase.client.datastore.model;

public class ComplexACL extends AccessControlList {
    private static final long serialVersionUID = -4257574164546267919L;

    private String listElementCode;

    public String getListElementCode() {
        return listElementCode;
    }

    public void setListElementCode(String listElementCode) {
        this.listElementCode = listElementCode;
    }
}
