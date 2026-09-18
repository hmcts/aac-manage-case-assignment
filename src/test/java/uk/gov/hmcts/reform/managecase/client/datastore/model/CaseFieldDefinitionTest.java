package uk.gov.hmcts.reform.managecase.client.datastore.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CaseFieldDefinitionTest {

    @Test
    void shouldFindAccessControlByRoleIgnoringCase() {
        AccessControlList acl = new AccessControlList();
        acl.setRole("Caseworker");

        CaseFieldDefinition field = new CaseFieldDefinition();
        field.setAccessControlLists(List.of(acl));

        assertThat(field.getAccessControlListByRole("caseWORKER")).containsSame(acl);
        assertThat(field.getAccessControlListByRole("Solicitor")).isEmpty();
    }

    @Test
    void shouldExposeComplexFieldChildren() {
        CaseFieldDefinition child = new CaseFieldDefinition();
        child.setId("AddressLine1");
        FieldTypeDefinition fieldType = new FieldTypeDefinition();
        fieldType.setType(FieldTypeDefinition.COMPLEX);
        fieldType.setComplexFields(List.of(child));

        assertThat(fieldType.getChildren()).containsExactly(child);
    }
}
