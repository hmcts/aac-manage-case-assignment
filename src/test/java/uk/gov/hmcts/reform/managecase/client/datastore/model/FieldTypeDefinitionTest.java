package uk.gov.hmcts.reform.managecase.client.datastore.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.TEXT;

class FieldTypeDefinitionTest {

    @Test
    void shouldReturnComplexFieldsAsChildrenForComplexFieldType() {
        CaseFieldDefinition firstNestedField = caseFieldDefinition("FirstNestedField", TEXT);
        CaseFieldDefinition secondNestedField = caseFieldDefinition("SecondNestedField", TEXT);
        FieldTypeDefinition fieldTypeDefinition = complexFieldTypeDefinition(firstNestedField, secondNestedField);

        List<CaseFieldDefinition> result = fieldTypeDefinition.getChildren();

        assertThat(result).containsExactly(firstNestedField, secondNestedField);
    }

    @Test
    void shouldReturnCollectionComplexFieldsAsChildrenForCollectionFieldType() {
        CaseFieldDefinition nestedField = caseFieldDefinition("CollectionNestedField", TEXT);
        FieldTypeDefinition fieldTypeDefinition = collectionFieldTypeDefinition(nestedField);

        List<CaseFieldDefinition> result = fieldTypeDefinition.getChildren();

        assertThat(result).containsExactly(nestedField);
    }

    @Test
    void shouldReturnEmptyChildrenForCollectionFieldTypeWithoutCollectionFieldTypeDefinition() {
        FieldTypeDefinition fieldTypeDefinition = fieldTypeDefinition(COLLECTION);

        List<CaseFieldDefinition> result = fieldTypeDefinition.getChildren();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyChildrenForSimpleFieldType() {
        FieldTypeDefinition fieldTypeDefinition = fieldTypeDefinition(TEXT);

        List<CaseFieldDefinition> result = fieldTypeDefinition.getChildren();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldSetChildrenForComplexFieldType() {
        CaseFieldDefinition nestedField = caseFieldDefinition("NestedField", TEXT);
        FieldTypeDefinition fieldTypeDefinition = fieldTypeDefinition(COMPLEX);

        fieldTypeDefinition.setChildren(List.of(nestedField));

        assertThat(fieldTypeDefinition.getComplexFields()).containsExactly(nestedField);
    }

    @Test
    void shouldSetChildrenForCollectionFieldType() {
        CaseFieldDefinition nestedField = caseFieldDefinition("NestedField", TEXT);
        FieldTypeDefinition collectionFieldTypeDefinition = fieldTypeDefinition(COLLECTION);
        collectionFieldTypeDefinition.setCollectionFieldTypeDefinition(fieldTypeDefinition(COMPLEX));

        collectionFieldTypeDefinition.setChildren(List.of(nestedField));

        assertThat(collectionFieldTypeDefinition.getChildren()).containsExactly(nestedField);
    }

    @Test
    void shouldNotSetChildrenForCollectionFieldTypeWithoutCollectionFieldTypeDefinition() {
        CaseFieldDefinition nestedField = caseFieldDefinition("NestedField", TEXT);
        FieldTypeDefinition fieldTypeDefinition = fieldTypeDefinition(COLLECTION);

        fieldTypeDefinition.setChildren(List.of(nestedField));

        assertThat(fieldTypeDefinition.getChildren()).isEmpty();
    }

    @Test
    void shouldNotSetChildrenForSimpleFieldType() {
        CaseFieldDefinition nestedField = caseFieldDefinition("NestedField", TEXT);
        FieldTypeDefinition fieldTypeDefinition = fieldTypeDefinition(TEXT);

        fieldTypeDefinition.setChildren(List.of(nestedField));

        assertThat(fieldTypeDefinition.getChildren()).isEmpty();
    }

    @Test
    void shouldIdentifyCollectionFieldTypeIgnoringCase() {
        FieldTypeDefinition fieldTypeDefinition = fieldTypeDefinition("collection");

        assertAll(
            () -> assertThat(fieldTypeDefinition.isCollectionFieldType()).isTrue(),
            () -> assertThat(fieldTypeDefinition.isComplexFieldType()).isFalse()
        );
    }

    @Test
    void shouldIdentifyComplexFieldTypeIgnoringCase() {
        FieldTypeDefinition fieldTypeDefinition = fieldTypeDefinition("complex");

        assertAll(
            () -> assertThat(fieldTypeDefinition.isCollectionFieldType()).isFalse(),
            () -> assertThat(fieldTypeDefinition.isComplexFieldType()).isTrue()
        );
    }

    @Test
    void shouldNotIdentifySimpleFieldTypeAsComplexOrCollection() {
        FieldTypeDefinition fieldTypeDefinition = fieldTypeDefinition(TEXT);

        assertAll(
            () -> assertThat(fieldTypeDefinition.isCollectionFieldType()).isFalse(),
            () -> assertThat(fieldTypeDefinition.isComplexFieldType()).isFalse()
        );
    }

    @Test
    void shouldFindNestedField() {
        CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", TEXT);
        FieldTypeDefinition fieldTypeDefinition = complexFieldTypeDefinition(nestedField);

        Optional<CommonField> result = fieldTypeDefinition.getNestedField("NestedTextField", false);

        assertThat(result).contains(nestedField);
    }

    @Test
    void shouldFindDeeplyNestedField() {
        CaseFieldDefinition deeplyNestedField = caseFieldDefinition("DeeplyNestedField", TEXT);
        CaseFieldDefinition nestedComplexField = complexCaseFieldDefinition("NestedComplexField", deeplyNestedField);
        FieldTypeDefinition fieldTypeDefinition = complexFieldTypeDefinition(nestedComplexField);

        Optional<CommonField> result = fieldTypeDefinition.getNestedField(
            "NestedComplexField.DeeplyNestedField", false);

        assertThat(result).contains(deeplyNestedField);
    }

    @Test
    void shouldFindNestedFieldWhenPathIncludesParent() {
        CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", TEXT);
        FieldTypeDefinition fieldTypeDefinition = complexFieldTypeDefinition(nestedField);

        Optional<CommonField> result = fieldTypeDefinition.getNestedField("ComplexField.NestedTextField", true);

        assertThat(result).contains(nestedField);
    }

    @Test
    void shouldReturnEmptyWhenNestedFieldDoesNotExist() {
        CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", TEXT);
        FieldTypeDefinition fieldTypeDefinition = complexFieldTypeDefinition(nestedField);

        Optional<CommonField> result = fieldTypeDefinition.getNestedField("MissingField", false);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNestedFieldPathIsBlank() {
        CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", TEXT);
        FieldTypeDefinition fieldTypeDefinition = complexFieldTypeDefinition(nestedField);

        Optional<CommonField> result = fieldTypeDefinition.getNestedField(" ", false);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnStringContainingFieldTypeDetails() {
        FieldTypeDefinition fieldTypeDefinition = fieldTypeDefinition(TEXT);
        fieldTypeDefinition.setId("TextFieldType");

        String result = fieldTypeDefinition.toString();

        assertThat(result).contains("TextFieldType", TEXT);
    }

    private CaseFieldDefinition complexCaseFieldDefinition(String id, CaseFieldDefinition... nestedFields) {
        return caseFieldDefinition(id, complexFieldTypeDefinition(nestedFields));
    }

    private CaseFieldDefinition caseFieldDefinition(String id, String fieldType) {
        return caseFieldDefinition(id, fieldTypeDefinition(fieldType));
    }

    private CaseFieldDefinition caseFieldDefinition(String id, FieldTypeDefinition fieldTypeDefinition) {
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setId(id);
        caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);

        return caseFieldDefinition;
    }

    private FieldTypeDefinition collectionFieldTypeDefinition(CaseFieldDefinition... nestedFields) {
        FieldTypeDefinition fieldTypeDefinition = fieldTypeDefinition(COLLECTION);
        fieldTypeDefinition.setCollectionFieldTypeDefinition(complexFieldTypeDefinition(nestedFields));

        return fieldTypeDefinition;
    }

    private FieldTypeDefinition complexFieldTypeDefinition(CaseFieldDefinition... nestedFields) {
        FieldTypeDefinition fieldTypeDefinition = fieldTypeDefinition(COMPLEX);
        fieldTypeDefinition.setComplexFields(List.of(nestedFields));

        return fieldTypeDefinition;
    }

    private FieldTypeDefinition fieldTypeDefinition(String type) {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setType(type);

        return fieldTypeDefinition;
    }
}
