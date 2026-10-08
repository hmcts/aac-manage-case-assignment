package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

@Getter
@Setter
@SuppressWarnings("unused")
public class FieldTypeDefinition implements Serializable {

    @Serial
    private static final long serialVersionUID = -4257574164546267919L;

    public static final String COLLECTION = "Collection";
    public static final String COMPLEX = "Complex";
    public static final String MULTI_SELECT_LIST = "MultiSelectList";
    public static final String FIXED_LIST = "FixedList";
    public static final String FIXED_RADIO_LIST = "FixedRadioList";
    public static final String LABEL = "Label";
    public static final String CASE_PAYMENT_HISTORY_VIEWER = "CasePaymentHistoryViewer";
    public static final String CASE_HISTORY_VIEWER = "CaseHistoryViewer";
    public static final String PREDEFINED_COMPLEX_ADDRESS_GLOBAL = "AddressGlobal";
    public static final String PREDEFINED_COMPLEX_ADDRESS_GLOBAL_UK = "AddressGlobalUK";
    public static final String PREDEFINED_COMPLEX_ADDRESS_UK = "AddressUK";
    public static final String PREDEFINED_COMPLEX_ORDER_SUMMARY = "OrderSummary";
    public static final String PREDEFINED_COMPLEX_CASELINK = "CaseLink";
    public static final String PREDEFINED_COMPLEX_ORGANISATION_POLICY = "OrganisationPolicy";
    public static final String PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST = "ChangeOrganisationRequest";
    public static final String DATETIME = "DateTime";
    public static final String DATE = "Date";
    public static final String TEXT = "Text";

    private String id;
    private String type;
    private BigDecimal min;
    private BigDecimal max;
    @JsonProperty("regular_expression")
    private String regularExpression;
    @JsonProperty("fixed_list_items")
    private List<FixedListItemDefinition> fixedListItemDefinitions = new ArrayList<>();
    @JsonProperty("complex_fields")
    private List<CaseFieldDefinition> complexFields = new ArrayList<>();
    @JsonProperty("collection_field_type")
    private FieldTypeDefinition collectionFieldTypeDefinition;

    @JsonIgnore
    public List<CaseFieldDefinition> getChildren() {
        if (isComplexFieldType()) {
            return complexFields;
        } else if (isCollectionFieldType()) {
            if (collectionFieldTypeDefinition == null) {
                return emptyList();
            }
            return collectionFieldTypeDefinition.complexFields;
        } else {
            return emptyList();
        }
    }

    @JsonIgnore
    public void setChildren(List<CaseFieldDefinition> caseFieldDefinitions) {
        if (type.equalsIgnoreCase(COMPLEX)) {
            complexFields = caseFieldDefinitions;
        } else if (type.equalsIgnoreCase(COLLECTION) && collectionFieldTypeDefinition != null) {
            collectionFieldTypeDefinition.complexFields = caseFieldDefinitions;
        }
    }

    @JsonIgnore
    public boolean isCollectionFieldType() {
        return type.equalsIgnoreCase(COLLECTION);
    }

    @JsonIgnore
    public boolean isComplexFieldType() {
        return type.equalsIgnoreCase(COMPLEX);
    }

    public Optional<CommonField> getNestedField(String path, boolean pathIncludesParent) {
        return CaseFieldPathUtils.getFieldDefinitionByPath(this, path, pathIncludesParent)
            .map(CommonField.class::cast);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
