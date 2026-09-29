package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.COMPLEX;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.LABEL;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.TEXT;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class CaseTypeDefinitionTest {

    @Test
    void shouldReturnJurisdictionId() {
        JurisdictionDefinition jurisdictionDefinition = new JurisdictionDefinition();
        jurisdictionDefinition.setId("BEFTA_MASTER");
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setJurisdictionDefinition(jurisdictionDefinition);

        assertThat(caseTypeDefinition.getJurisdictionId(), is("BEFTA_MASTER"));
    }

    @Test
    void shouldReturnSecurityClassificationForField() {
        CaseFieldDefinition caseFieldDefinition = caseFieldDefinition("TextField", TEXT);
        caseFieldDefinition.setSecurityLabel("PRIVATE");
        CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(caseFieldDefinition);

        SecurityClassification result = caseTypeDefinition.getClassificationForField("TextField");

        assertThat(result, is(SecurityClassification.PRIVATE));
    }

    @Test
    void shouldThrowExceptionWhenFieldClassificationDoesNotExist() {
        CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(caseFieldDefinition("TextField", TEXT));
        caseTypeDefinition.setName("CaseType");

        RuntimeException result = assertThrows(RuntimeException.class,
            () -> caseTypeDefinition.getClassificationForField("MissingField"));

        assertThat(result.getMessage(), is("CaseFieldId MissingField not found in CaseType CaseType"));
    }

    @Test
    void shouldReturnTrueWhenAnyEventCanSaveDraft() {
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setEvents(List.of(
            caseEventDefinition("Submit", null),
            caseEventDefinition("Update", false),
            caseEventDefinition("Review", true)
        ));

        assertThat(caseTypeDefinition.hasDraftEnabledEvent(), is(true));
    }

    @Test
    void shouldReturnFalseWhenNoEventsCanSaveDraft() {
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setEvents(List.of(
            caseEventDefinition("Submit", null),
            caseEventDefinition("Update", false)
        ));

        assertThat(caseTypeDefinition.hasDraftEnabledEvent(), is(false));
    }

    @Test
    void shouldReturnTrueWhenEventIdExists() {
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setEvents(List.of(caseEventDefinition("submitCase", false)));

        assertThat(caseTypeDefinition.hasEventId("submitCase"), is(true));
    }

    @Test
    void shouldReturnFalseWhenEventIdDoesNotExist() {
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setEvents(List.of(caseEventDefinition("submitCase", false)));

        assertThat(caseTypeDefinition.hasEventId("missingEvent"), is(false));
    }

    @Test
    void shouldFindCaseEventIgnoringCase() {
        CaseEventDefinition caseEventDefinition = caseEventDefinition("submitCase", false);
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setEvents(List.of(caseEventDefinition));

        Optional<CaseEventDefinition> result = caseTypeDefinition.findCaseEvent("SUBMITCASE");

        assertThat(result, is(Optional.of(caseEventDefinition)));
    }

    @Test
    void shouldReturnEmptyWhenCaseEventDoesNotExist() {
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setEvents(List.of(caseEventDefinition("submitCase", false)));

        Optional<CaseEventDefinition> result = caseTypeDefinition.findCaseEvent("missingEvent");

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void shouldAddSearchAliasFields() {
        SearchAliasField firstSearchAliasField = searchAliasField("SearchAliasOne");
        SearchAliasField secondSearchAliasField = searchAliasField("SearchAliasTwo");
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setSearchAliasFields(List.of(firstSearchAliasField));

        caseTypeDefinition.setSearchAliasFields(List.of(secondSearchAliasField));

        assertThat(caseTypeDefinition.getSearchAliasFields(),
            is(List.of(firstSearchAliasField, secondSearchAliasField)));
    }

    @Test
    void shouldIgnoreNullSearchAliasFields() {
        SearchAliasField searchAliasField = searchAliasField("SearchAlias");
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setSearchAliasFields(List.of(searchAliasField));

        caseTypeDefinition.setSearchAliasFields(null);

        assertThat(caseTypeDefinition.getSearchAliasFields(), is(List.of(searchAliasField)));
    }

    @Test
    void shouldReturnTrueWhenCaseFieldIsACollection() {
        CaseFieldDefinition collectionField = caseFieldDefinition("CollectionField", COLLECTION);
        CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(collectionField);

        assertThat(caseTypeDefinition.isCaseFieldACollection("CollectionField"), is(true));
    }

    @Test
    void shouldReturnFalseWhenCaseFieldIsNotACollection() {
        CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(caseFieldDefinition("TextField", TEXT));

        assertThat(caseTypeDefinition.isCaseFieldACollection("TextField"), is(false));
    }

    @Test
    void shouldReturnFalseWhenCollectionCaseFieldDoesNotExist() {
        CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(caseFieldDefinition("TextField", TEXT));

        assertThat(caseTypeDefinition.isCaseFieldACollection("MissingField"), is(false));
    }

    @Test
    void shouldFindCaseFieldIgnoringCase() {
        CaseFieldDefinition caseFieldDefinition = caseFieldDefinition("TextField", TEXT);
        CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(caseFieldDefinition);

        Optional<CaseFieldDefinition> result = caseTypeDefinition.getCaseField("textfield");

        assertThat(result, is(Optional.of(caseFieldDefinition)));
    }

    @Test
    void shouldReturnEmptyWhenCaseFieldDoesNotExist() {
        CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(caseFieldDefinition("TextField", TEXT));

        Optional<CaseFieldDefinition> result = caseTypeDefinition.getCaseField("MissingField");

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void shouldFindComplexSubfieldDefinitionByPath() {
        CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", TEXT);
        CaseFieldDefinition complexField = complexCaseFieldDefinition("ComplexField", nestedField);
        CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(complexField);

        Optional<CaseFieldDefinition> result = caseTypeDefinition.getComplexSubfieldDefinitionByPath(
            "ComplexField.NestedTextField");

        assertThat(result, is(Optional.of(nestedField)));
    }

    @Test
    void shouldReturnEmptyWhenComplexSubfieldDefinitionPathDoesNotExist() {
        CaseFieldDefinition nestedField = caseFieldDefinition("NestedTextField", TEXT);
        CaseFieldDefinition complexField = complexCaseFieldDefinition("ComplexField", nestedField);
        CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(complexField);

        Optional<CaseFieldDefinition> result = caseTypeDefinition.getComplexSubfieldDefinitionByPath(
            "ComplexField.MissingField");

        assertThat(result.isEmpty(), is(true));
    }

    @Test
    void shouldReturnLabelsFromLabelCaseFields() {
        CaseFieldDefinition labelField = caseFieldDefinition("LabelField", LABEL);
        labelField.setLabel("The label text");
        CaseFieldDefinition textField = caseFieldDefinition("TextField", TEXT);
        textField.setLabel("The text field label");
        CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(labelField, textField);

        Map<String, TextNode> result = caseTypeDefinition.getLabelsFromCaseFields();

        assertAll(
            () -> assertThat(result.size(), is(1)),
            () -> assertThat(result.get("LabelField").asText(), is("The label text"))
        );
    }

    @Test
    void shouldReturnEmptyMapWhenNoLabelCaseFieldsExist() {
        CaseTypeDefinition caseTypeDefinition = caseTypeDefinition(caseFieldDefinition("TextField", TEXT));

        Map<String, TextNode> result = caseTypeDefinition.getLabelsFromCaseFields();

        assertThat(result.isEmpty(), is(true));
    }

    private CaseTypeDefinition caseTypeDefinition(CaseFieldDefinition... caseFieldDefinitions) {
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(List.of(caseFieldDefinitions));

        return caseTypeDefinition;
    }

    private CaseEventDefinition caseEventDefinition(String id, Boolean canSaveDraft) {
        CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId(id);
        caseEventDefinition.setCanSaveDraft(canSaveDraft);

        return caseEventDefinition;
    }

    private SearchAliasField searchAliasField(String id) {
        SearchAliasField searchAliasField = new SearchAliasField();
        searchAliasField.setId(id);

        return searchAliasField;
    }

    private CaseFieldDefinition complexCaseFieldDefinition(String id, CaseFieldDefinition... nestedFields) {
        FieldTypeDefinition fieldTypeDefinition = fieldTypeDefinition(COMPLEX);
        fieldTypeDefinition.setComplexFields(List.of(nestedFields));

        return caseFieldDefinition(id, fieldTypeDefinition);
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

    private FieldTypeDefinition fieldTypeDefinition(String type) {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setType(type);

        return fieldTypeDefinition;
    }
}

