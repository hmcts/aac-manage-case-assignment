package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.TooManyMethods"})
class CaseFieldPathUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    class GetNestedCaseFieldByPathTest {

        @Test
        void shouldFindTopLevelField() throws JsonProcessingException {
            Map<String, JsonNode> caseData = createCaseData();
            JsonNode result = CaseFieldPathUtils.getNestedCaseFieldByPath(caseData, "TextField");

            assertAll(
                () -> assertThat(result.isTextual(), is(true)),
                () -> assertThat(result.asText(), is("TextValue"))
            );
        }

        @Test
        void shouldFindNestedField() throws JsonProcessingException {
            Map<String, JsonNode> caseData = createCaseData();
            JsonNode result = CaseFieldPathUtils.getNestedCaseFieldByPath(caseData,
                "ComplexField.ComplexNestedField.NestedNumberField");

            assertAll(
                () -> assertThat(result.isTextual(), is(true)),
                () -> assertThat(result.asText(), is("67890"))
            );
        }

        @Test
        void shouldFindNestedFieldWhenPathContainsWhitespace() throws JsonProcessingException {
            Map<String, JsonNode> caseData = createCaseData();
            JsonNode result = CaseFieldPathUtils.getNestedCaseFieldByPath(caseData,
                " ComplexField.ComplexNestedField.NestedNumberField ");

            assertAll(
                () -> assertThat(result.isTextual(), is(true)),
                () -> assertThat(result.asText(), is("67890"))
            );
        }

        @Test
        void shouldFindNestedFieldFromJsonNode() throws JsonProcessingException {
            JsonNode complexField = createCaseData().get("ComplexField");
            JsonNode result = CaseFieldPathUtils.getNestedCaseFieldByPath(complexField,
                "ComplexNestedField.NestedNumberField");

            assertAll(
                () -> assertThat(result.isTextual(), is(true)),
                () -> assertThat(result.asText(), is("67890"))
            );
        }

        @Test
        void shouldFindNullNode() throws JsonProcessingException {
            Map<String, JsonNode> caseData = createCaseData();
            JsonNode result = CaseFieldPathUtils.getNestedCaseFieldByPath(caseData,"TextAreaField");

            assertAll(
                () -> assertThat(result.isNull(), is(true))
            );
        }

        @Test
        void shouldReturnNullForNonExistingNode() throws JsonProcessingException {
            Map<String, JsonNode> caseData = createCaseData();
            JsonNode result = CaseFieldPathUtils.getNestedCaseFieldByPath(caseData,"NonExisting");

            assertAll(
                () -> assertThat(result, is(nullValue()))
            );
        }

        @Test
        void shouldReturnNullForNonExistingNestedNode() throws JsonProcessingException {
            Map<String, JsonNode> caseData = createCaseData();
            JsonNode result = CaseFieldPathUtils.getNestedCaseFieldByPath(caseData,
                "ComplexField.ComplexNestedField.MissingField");

            assertThat(result, is(nullValue()));
        }

        private Map<String, JsonNode> createCaseData() throws JsonProcessingException {
            return objectMapper.readValue(caseDataString(), new TypeReference<>() {
            });
        }

        private String caseDataString() {
            return """
                {
                    "DateField": "1985-07-25",
                    "TextField": "TextValue",
                    "EmailField": "test@email.com",
                    "NumberField": "12345",
                    "ComplexField": {
                        "ComplexNestedField": {
                            "NestedNumberField": "67890",
                            "NestedCollectionTextField": []
                        },
                        "ComplexTextField": "ComplexTextValue"
                    },
                    "PhoneUKField": "01234 567890",
                    "YesOrNoField": "No",
                    "DateTimeField": "2020-12-15T12:30:15.000",
                    "MoneyGBPField": "25000",
                    "TextAreaField": null,
                    "AddressUKField": {
                        "County": "CountValue",
                        "Country": "CountryValue",
                        "PostCode": "PST CDE",
                        "PostTown": "TownValue",
                        "AddressLine1": "BuildingValue",
                        "AddressLine2": "AddressLine2Value",
                        "AddressLine3": "AddressLine3Value"
                    }
                }""";
        }
    }

    @Nested
    class GetFieldDefinitionByPathTest {

        @Test
        void shouldFindTopLevelCaseFieldDefinition() {
            CaseFieldDefinition textField = caseFieldDefinition("TextField", fieldType("Text"));
            CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(textField);

            Optional<CaseFieldDefinition> result = CaseFieldPathUtils.getFieldDefinitionByPath(caseTypeDefinition,
                "TextField");

            assertThat(result, is(Optional.of(textField)));
        }

        @Test
        void shouldFindNestedCaseFieldDefinition() {
            CaseFieldDefinition nestedNumberField = caseFieldDefinition("NestedNumberField", fieldType("Number"));
            CaseFieldDefinition nestedComplexField = complexCaseFieldDefinition("NestedComplexField",
                nestedNumberField);
            CaseFieldDefinition complexField = complexCaseFieldDefinition("ComplexField", nestedComplexField);
            CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(complexField);

            Optional<CaseFieldDefinition> result = CaseFieldPathUtils.getFieldDefinitionByPath(caseTypeDefinition,
                "ComplexField.NestedComplexField.NestedNumberField");

            assertThat(result, is(Optional.of(nestedNumberField)));
        }

        @Test
        void shouldFindCaseFieldDefinitionWhenPathContainsWhitespace() {
            CaseFieldDefinition textField = caseFieldDefinition("TextField", fieldType("Text"));
            CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(textField);

            Optional<CaseFieldDefinition> result = CaseFieldPathUtils.getFieldDefinitionByPath(caseTypeDefinition,
                " TextField ");

            assertThat(result, is(Optional.of(textField)));
        }

        @Test
        void shouldFindNestedCollectionCaseFieldDefinition() {
            CaseFieldDefinition collectionTextField = caseFieldDefinition("CollectionTextField", fieldType("Text"));
            CaseFieldDefinition collectionField = collectionCaseFieldDefinition("CollectionField", collectionTextField);
            CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(collectionField);

            Optional<CaseFieldDefinition> result = CaseFieldPathUtils.getFieldDefinitionByPath(caseTypeDefinition,
                "CollectionField.CollectionTextField");

            assertThat(result, is(Optional.of(collectionTextField)));
        }

        @Test
        void shouldReturnEmptyWhenCaseTypePathIsBlank() {
            CaseFieldDefinition textField = caseFieldDefinition("TextField", fieldType("Text"));
            CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(textField);

            Optional<CaseFieldDefinition> result = CaseFieldPathUtils.getFieldDefinitionByPath(caseTypeDefinition, " ");

            assertThat(result.isEmpty(), is(true));
        }

        @Test
        void shouldReturnEmptyWhenTopLevelCaseFieldDefinitionDoesNotExist() {
            CaseFieldDefinition textField = caseFieldDefinition("TextField", fieldType("Text"));
            CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(textField);

            Optional<CaseFieldDefinition> result = CaseFieldPathUtils.getFieldDefinitionByPath(caseTypeDefinition,
                "MissingField");

            assertThat(result.isEmpty(), is(true));
        }

        @Test
        void shouldReturnEmptyWhenNestedCaseFieldDefinitionDoesNotExist() {
            CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", fieldType("Text"));
            CaseFieldDefinition complexField = complexCaseFieldDefinition("ComplexField", nestedField);
            CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(complexField);

            Optional<CaseFieldDefinition> result = CaseFieldPathUtils.getFieldDefinitionByPath(caseTypeDefinition,
                "ComplexField.MissingField");

            assertThat(result.isEmpty(), is(true));
        }

        @Test
        void shouldReturnCurrentCaseFieldDefinitionWhenNestedPathIsBlank() {
            CaseFieldDefinition complexField = complexCaseFieldDefinition("ComplexField");

            Optional<CaseFieldDefinition> result = CaseFieldPathUtils.getFieldDefinitionByPath(complexField, " ");

            assertThat(result, is(Optional.of(complexField)));
        }

        @Test
        void shouldFindNestedCaseFieldDefinitionFromFieldDefinition() {
            CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", fieldType("Text"));
            CaseFieldDefinition complexField = complexCaseFieldDefinition("ComplexField", nestedField);

            Optional<CaseFieldDefinition> result = CaseFieldPathUtils.getFieldDefinitionByPath(complexField,
                "NestedTextField");

            assertThat(result, is(Optional.of(nestedField)));
        }

        @Test
        void shouldFindNestedCommonFieldDefinition() {
            CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", fieldType("Text"));
            CommonField complexField = complexCaseFieldDefinition("ComplexField", nestedField);

            Optional<CommonField> result = CaseFieldPathUtils.getFieldDefinitionByPath(complexField,
                "NestedTextField");

            assertThat(result, is(Optional.of(nestedField)));
        }

        @Test
        void shouldFindNestedCaseFieldDefinitionWhenPathIncludesParent() {
            CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", fieldType("Text"));
            FieldTypeDefinition complexFieldType = complexFieldTypeDefinition(nestedField);

            Optional<CaseFieldDefinition> result = CaseFieldPathUtils.getFieldDefinitionByPath(complexFieldType,
                "ComplexField.NestedTextField", true);

            assertThat(result, is(Optional.of(nestedField)));
        }

        @Test
        void shouldReturnEmptyWhenPathIncludesParentButHasNoNestedField() {
            CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", fieldType("Text"));
            FieldTypeDefinition complexFieldType = complexFieldTypeDefinition(nestedField);

            Optional<CaseFieldDefinition> result = CaseFieldPathUtils.getFieldDefinitionByPath(complexFieldType,
                "ComplexField", true);

            assertThat(result.isEmpty(), is(true));
        }

        @Test
        void shouldReturnEmptyWhenFieldTypeHasNoNestedFields() {
            Optional<CaseFieldDefinition> result = CaseFieldPathUtils.getFieldDefinitionByPath(fieldType("Text"),
                "NestedTextField", false);

            assertThat(result.isEmpty(), is(true));
        }
    }

    private CaseTypeDefinition caseTypeDefinition(CaseFieldDefinition... caseFieldDefinitions) {
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(List.of(caseFieldDefinitions));

        return caseTypeDefinition;
    }

    private CaseFieldDefinition complexCaseFieldDefinition(String id, CaseFieldDefinition... nestedFields) {
        return caseFieldDefinition(id, complexFieldTypeDefinition(nestedFields));
    }

    private CaseFieldDefinition collectionCaseFieldDefinition(String id, CaseFieldDefinition... nestedFields) {
        FieldTypeDefinition fieldTypeDefinition = fieldType("Collection");
        fieldTypeDefinition.setCollectionFieldTypeDefinition(complexFieldTypeDefinition(nestedFields));

        return caseFieldDefinition(id, fieldTypeDefinition);
    }

    private CaseFieldDefinition caseFieldDefinition(String id, FieldTypeDefinition fieldTypeDefinition) {
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setId(id);
        caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);

        return caseFieldDefinition;
    }

    private FieldTypeDefinition complexFieldTypeDefinition(CaseFieldDefinition... nestedFields) {
        FieldTypeDefinition fieldTypeDefinition = fieldType("Complex");
        fieldTypeDefinition.setComplexFields(List.of(nestedFields));

        return fieldTypeDefinition;
    }

    private FieldTypeDefinition fieldType(String type) {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setType(type);

        return fieldTypeDefinition;
    }
}
