package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaseFieldDefinitionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerialiseFormattedValue() throws JsonProcessingException {
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        JsonNode formattedValue = objectMapper.readTree("{\"code\":\"A\",\"label\":\"Accepted\"}");
        caseFieldDefinition.setFormattedValue(formattedValue);

        String json = objectMapper.writeValueAsString(caseFieldDefinition);
        CaseFieldDefinition result = objectMapper.readValue(json, CaseFieldDefinition.class);

        assertThat(objectMapper.valueToTree(result.getFormattedValue()), is(formattedValue));
    }

    @Test
    void shouldIdentifyCollectionFieldType() {
        CaseFieldDefinition caseFieldDefinition = caseFieldDefinition("CollectionField", fieldType("Collection"));

        assertAll(
            () -> assertThat(caseFieldDefinition.isCollectionFieldType(), is(true)),
            () -> assertThat(caseFieldDefinition.isComplexFieldType(), is(false)),
            () -> assertThat(caseFieldDefinition.isCompoundFieldType(), is(true))
        );
    }

    @Test
    void shouldIdentifyComplexFieldType() {
        CaseFieldDefinition caseFieldDefinition = caseFieldDefinition("ComplexField", fieldType("Complex"));

        assertAll(
            () -> assertThat(caseFieldDefinition.isCollectionFieldType(), is(false)),
            () -> assertThat(caseFieldDefinition.isComplexFieldType(), is(true)),
            () -> assertThat(caseFieldDefinition.isCompoundFieldType(), is(true))
        );
    }

    @Test
    void shouldNotIdentifySimpleFieldTypeAsCompound() {
        CaseFieldDefinition caseFieldDefinition = caseFieldDefinition("TextField", fieldType("Text"));

        assertAll(
            () -> assertThat(caseFieldDefinition.isCollectionFieldType(), is(false)),
            () -> assertThat(caseFieldDefinition.isComplexFieldType(), is(false)),
            () -> assertThat(caseFieldDefinition.isCompoundFieldType(), is(false))
        );
    }

    @Test
    void shouldReturnDisplayContextTypeWhenDisplayContextIsPresent() {
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setDisplayContext("MANDATORY");

        assertThat(caseFieldDefinition.displayContextType(), is(DisplayContext.MANDATORY));
    }

    @Test
    void shouldReturnNullDisplayContextTypeWhenDisplayContextIsHidden() {
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setDisplayContext("HIDDEN");

        assertThat(caseFieldDefinition.displayContextType(), is(nullValue()));
    }

    @Test
    void shouldReturnNullDisplayContextTypeWhenDisplayContextIsMissing() {
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();

        assertThat(caseFieldDefinition.displayContextType(), is(nullValue()));
    }

    @Test
    void shouldRejectUnknownDisplayContextType() {
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setDisplayContext("UNKNOWN");

        assertThrows(IllegalArgumentException.class, caseFieldDefinition::displayContextType);
    }

    @Test
    void shouldReturnCurrentFieldWhenNestedFieldPathIsBlank() {
        CaseFieldDefinition caseFieldDefinition = caseFieldDefinition("ComplexField", fieldType("Complex"));

        Optional<CommonField> result = caseFieldDefinition.getComplexFieldNestedField(" ");

        assertThat(result, is(Optional.of(caseFieldDefinition)));
    }

    @Test
    void shouldFindDirectNestedFieldByPath() {
        CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", fieldType("Text"));
        CaseFieldDefinition caseFieldDefinition = complexCaseFieldDefinition("ComplexField", nestedField);

        Optional<CommonField> result = caseFieldDefinition.getComplexFieldNestedField("NestedTextField");

        assertThat(result, is(Optional.of(nestedField)));
    }

    @Test
    void shouldFindDeeplyNestedFieldByPath() {
        CaseFieldDefinition deeplyNestedField = caseFieldDefinition("NestedNumberField", fieldType("Number"));
        CaseFieldDefinition nestedField = complexCaseFieldDefinition("NestedComplexField", deeplyNestedField);
        CaseFieldDefinition caseFieldDefinition = complexCaseFieldDefinition("ComplexField", nestedField);

        Optional<CommonField> result = caseFieldDefinition.getComplexFieldNestedField(
            "NestedComplexField.NestedNumberField");

        assertThat(result, is(Optional.of(deeplyNestedField)));
    }

    @Test
    void shouldReturnEmptyWhenNestedFieldPathDoesNotExist() {
        CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", fieldType("Text"));
        CaseFieldDefinition caseFieldDefinition = complexCaseFieldDefinition("ComplexField", nestedField);

        Optional<CommonField> result = caseFieldDefinition.getComplexFieldNestedField("MissingField");

        assertThat(result.isEmpty(), is(true));
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
