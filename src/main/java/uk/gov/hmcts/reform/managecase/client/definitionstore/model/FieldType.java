package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Schema
@Builder
public class FieldType {

    private String id;
    private String type;
    private String min;
    private String max;
    private String regularExpression;
    private List<FixedListItem> fixedListItems;
    private List<CaseField> complexFields;
    private FieldType collectionFieldType;

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Schema
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("min")
    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    @JsonProperty("max")
    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    @JsonProperty("regular_expression")
    public String getRegularExpression() {
        return regularExpression;
    }

    public void setRegularExpression(String regularExpression) {
        this.regularExpression = regularExpression;
    }

    @JsonProperty("fixed_list_items")
    public List<FixedListItem> getFixedListItems() {
        return fixedListItems;
    }

    public void setFixedListItems(List<FixedListItem> fixedListItems) {
        this.fixedListItems = fixedListItems;
    }

    @JsonProperty("complex_fields")
    public List<CaseField> getComplexFields() {
        return complexFields;
    }

    public void setComplexFields(List<CaseField> complexFields) {
        if (complexFields != null) {
            complexFields.stream().forEach(cf -> cf.setAcls(null));
        }
        this.complexFields = complexFields;
    }

    @JsonProperty("collection_field_type")
    public FieldType getCollectionFieldType() {
        return collectionFieldType;
    }

    public void setCollectionFieldType(FieldType collectionFieldType) {
        this.collectionFieldType = collectionFieldType;
    }
}
