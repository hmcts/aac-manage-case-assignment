package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class CaseFieldDefinitionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerialiseFormattedValue() throws JsonProcessingException {
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        JsonNode formattedValue = objectMapper.readTree("{\"code\":\"A\",\"label\":\"Accepted\"}");
        caseFieldDefinition.setFormattedValue(formattedValue);

        String json = objectMapper.writeValueAsString(caseFieldDefinition);
        CaseFieldDefinition result = objectMapper.readValue(json, CaseFieldDefinition.class);

        assertThat(objectMapper.<JsonNode>valueToTree(result.getFormattedValue())).isEqualTo(formattedValue);
    }

    @Test
    void shouldIdentifyCollectionFieldType() {
        CaseFieldDefinition caseFieldDefinition = caseFieldDefinition("CollectionField", fieldType("Collection"));

        assertAll(
            () -> assertThat(caseFieldDefinition.isCollectionFieldType()).isTrue(),
            () -> assertThat(caseFieldDefinition.isComplexFieldType()).isFalse(),
            () -> assertThat(caseFieldDefinition.isCompoundFieldType()).isTrue()
        );
    }

    @Test
    void shouldIdentifyComplexFieldType() {
        CaseFieldDefinition caseFieldDefinition = caseFieldDefinition("ComplexField", fieldType("Complex"));

        assertAll(
            () -> assertThat(caseFieldDefinition.isCollectionFieldType()).isFalse(),
            () -> assertThat(caseFieldDefinition.isComplexFieldType()).isTrue(),
            () -> assertThat(caseFieldDefinition.isCompoundFieldType()).isTrue()
        );
    }

    @Test
    void shouldNotIdentifySimpleFieldTypeAsCompound() {
        CaseFieldDefinition caseFieldDefinition = caseFieldDefinition("TextField", fieldType("Text"));

        assertAll(
            () -> assertThat(caseFieldDefinition.isCollectionFieldType()).isFalse(),
            () -> assertThat(caseFieldDefinition.isComplexFieldType()).isFalse(),
            () -> assertThat(caseFieldDefinition.isCompoundFieldType()).isFalse()
        );
    }

    @Test
    void shouldReturnDisplayContextTypeWhenDisplayContextIsPresent() {
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setDisplayContext("MANDATORY");

        assertThat(caseFieldDefinition.displayContextType()).isEqualTo(DisplayContext.MANDATORY);
    }

    @Test
    void shouldReturnNullDisplayContextTypeWhenDisplayContextIsHidden() {
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setDisplayContext("HIDDEN");

        assertThat(caseFieldDefinition.displayContextType()).isNull();
    }

    @Test
    void shouldReturnNullDisplayContextTypeWhenDisplayContextIsMissing() {
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();

        assertThat(caseFieldDefinition.displayContextType()).isNull();
    }

    @Test
    void shouldRejectUnknownDisplayContextType() {
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setDisplayContext("UNKNOWN");

        assertThatThrownBy(caseFieldDefinition::displayContextType)
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnCurrentFieldWhenNestedFieldPathIsBlank() {
        CaseFieldDefinition caseFieldDefinition = caseFieldDefinition("ComplexField", fieldType("Complex"));

        Optional<CommonField> result = caseFieldDefinition.getComplexFieldNestedField(" ");

        assertThat(result).contains(caseFieldDefinition);
    }

    @Test
    void shouldFindDirectNestedFieldByPath() {
        CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", fieldType("Text"));
        CaseFieldDefinition caseFieldDefinition = complexCaseFieldDefinition("ComplexField", nestedField);

        Optional<CommonField> result = caseFieldDefinition.getComplexFieldNestedField("NestedTextField");

        assertThat(result).contains(nestedField);
    }

    @Test
    void shouldFindDeeplyNestedFieldByPath() {
        CaseFieldDefinition deeplyNestedField = caseFieldDefinition("NestedNumberField", fieldType("Number"));
        CaseFieldDefinition nestedField = complexCaseFieldDefinition("NestedComplexField", deeplyNestedField);
        CaseFieldDefinition caseFieldDefinition = complexCaseFieldDefinition("ComplexField", nestedField);

        Optional<CommonField> result = caseFieldDefinition.getComplexFieldNestedField(
            "NestedComplexField.NestedNumberField");

        assertThat(result).contains(deeplyNestedField);
    }

    @Test
    void shouldReturnEmptyWhenNestedFieldPathDoesNotExist() {
        CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", fieldType("Text"));
        CaseFieldDefinition caseFieldDefinition = complexCaseFieldDefinition("ComplexField", nestedField);

        Optional<CommonField> result = caseFieldDefinition.getComplexFieldNestedField("MissingField");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldExposeComplexFieldChildren() {
        CaseFieldDefinition child = caseFieldDefinition("AddressLine1", fieldType("Text"));
        FieldTypeDefinition complexType = fieldType("Complex");
        complexType.setComplexFields(List.of(child));

        assertThat(complexType.getChildren()).containsExactly(child);
    }

    private CaseFieldDefinition complexCaseFieldDefinition(String id, CaseFieldDefinition... nestedFields) {
        FieldTypeDefinition fieldTypeDefinition = fieldType("Complex");
        fieldTypeDefinition.setComplexFields(List.of(nestedFields));

        return caseFieldDefinition(id, fieldTypeDefinition);
    }

    private CaseFieldDefinition caseFieldDefinition(String id, FieldTypeDefinition fieldTypeDefinition) {
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setId(id);
        caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);

        return caseFieldDefinition;
    }

    private FieldTypeDefinition fieldType(String type) {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setType(type);

        return fieldTypeDefinition;
    }
}
