package uk.gov.hmcts.reform.managecase.client.datastore.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CaseViewFieldTest {

    @Test
    void shouldCreateViewFieldFromDefinitionAndCaseData() {
        FieldTypeDefinition type = new FieldTypeDefinition();
        type.setRegularExpression("[A-Z]+");
        AccessControlList acl = new AccessControlList();
        acl.setRole("caseworker");

        CaseFieldDefinition definition = new CaseFieldDefinition();
        definition.setId("FirstName");
        definition.setLabel("First name");
        definition.setFieldTypeDefinition(type);
        definition.setHidden(false);
        definition.setHintText("Enter a first name");
        definition.setSecurityLabel("PUBLIC");
        definition.setAccessControlLists(List.of(acl));
        definition.setMetadata(true);
        definition.setRetainHiddenValue(true);

        CaseViewField field = CaseViewField.createFrom(definition, Map.of("FirstName", "Ada"));

        assertThat(field.getId()).isEqualTo("FirstName");
        assertThat(field.getLabel()).isEqualTo("First name");
        assertThat(field.getValue()).isEqualTo("Ada");
        assertThat(field.getValidationExpression()).isEqualTo("[A-Z]+");
        assertThat(field.getAccessControlLists()).containsExactly(acl);
        assertThat(field.isMetadata()).isTrue();
        assertThat(field.getRetainHiddenValue()).isTrue();
    }

    @Test
    void shouldApplyTabFieldPresentationProperties() {
        CaseFieldDefinition definition = new CaseFieldDefinition();
        definition.setId("Address");
        FieldTypeDefinition type = new FieldTypeDefinition();
        definition.setFieldTypeDefinition(type);

        CaseTypeTabField tabField = new CaseTypeTabField();
        tabField.setCaseFieldDefinition(definition);
        tabField.setDisplayOrder(3);
        tabField.setShowCondition("Address != null");
        tabField.setDisplayContextParameter("#TABLE(Address)");

        CaseViewField field = CaseViewField.createFrom(tabField, Map.of());

        assertThat(field.getOrder()).isEqualTo(3);
        assertThat(field.getShowCondition()).isEqualTo("Address != null");
        assertThat(field.getDisplayContextParameter()).isEqualTo("#TABLE(Address)");
    }
}
