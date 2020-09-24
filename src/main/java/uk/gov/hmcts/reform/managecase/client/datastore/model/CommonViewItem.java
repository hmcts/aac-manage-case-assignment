package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public interface CommonViewItem {

    String getCaseId();

    Map<String, JsonNode> getFields();

    Map<String, JsonNode> getFieldsFormatted();
}
