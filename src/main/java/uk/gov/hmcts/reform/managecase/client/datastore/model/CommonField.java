package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public interface CommonField extends CommonDCPModel {

    FieldTypeDefinition getFieldTypeDefinition();

    String getId();

    List<AccessControlList> getAccessControlLists();

    String getDisplayContext();

    void setDisplayContext(String displayContext);

    void setDisplayContextParameter(String displayContextParameter);

    Object getFormattedValue();

    void setFormattedValue(Object formattedValue);

    @JsonIgnore
    default boolean isCollectionFieldType() {
        return getFieldTypeDefinition().isCollectionFieldType();
    }

    @JsonIgnore
    default boolean isComplexFieldType() {
        return getFieldTypeDefinition().isComplexFieldType();
    }

    @JsonIgnore
    default boolean isCompoundFieldType() {
        return isCollectionFieldType() || isComplexFieldType();
    }

    default DisplayContext displayContextType() {
        return Optional.ofNullable(getDisplayContext())
            .filter(dc -> !"HIDDEN".equals(dc))
            .map(DisplayContext::valueOf)
            .orElse(null);
    }

    /**
     * Gets a caseField by specified path.
     *
     * @param path Path to a nested CaseField
     * @return A nested CaseField or 'this' when path is blank
     */
    @JsonIgnore
    default Optional<CommonField> getComplexFieldNestedField(String path) {
        return CaseFieldPathUtils.getFieldDefinitionByPath(this, path);
    }

}
