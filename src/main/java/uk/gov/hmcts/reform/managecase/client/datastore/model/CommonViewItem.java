package uk.gov.hmcts.reform.managecase.client.datastore.model;

import java.util.Map;

public interface CommonViewItem {

    String getCaseId();

    Map<String, Object> getFields();

    Map<String, Object> getFieldsFormatted();
}
