package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import uk.gov.hmcts.reform.managecase.client.datastore.model.AccessControlList;

import java.util.List;

public interface HasAcls {

    void setAcls(List<AccessControlList> acls);

}
