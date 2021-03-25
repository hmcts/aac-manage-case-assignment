package uk.gov.hmcts.reform.managecase.client.datastore.model;


public interface CommonViewHeader extends CommonDCPModel {

    String getCaseFieldId();

    FieldTypeDefinition getCaseFieldTypeDefinition();

    boolean isMetadata();
}
