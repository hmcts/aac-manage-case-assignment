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

    @Test
    void shouldPropagateAndApplyComplexAclsToNestedFields() {
        CaseFieldDefinition firstChild = complexChild("First");
        CaseFieldDefinition secondChild = complexChild("Second");
        CaseFieldDefinition root = complexChild("Root");
        root.getFieldTypeDefinition().setComplexFields(List.of(firstChild, secondChild));

        AccessControlList inheritedAcl = new AccessControlList();
        inheritedAcl.setRole("caseworker");
        inheritedAcl.setRead(true);
        root.setAccessControlLists(List.of(inheritedAcl));

        ComplexACL firstChildAcl = new ComplexACL();
        firstChildAcl.setRole("caseworker");
        firstChildAcl.setListElementCode("First");
        firstChildAcl.setUpdate(true);
        root.setComplexACLs(List.of(firstChildAcl));

        root.propagateACLsToNestedFields();

        assertThat(firstChild.getAccessControlListByRole("caseworker")).containsSame(firstChildAcl);
        assertThat(firstChild.getAccessControlLists()).containsExactly(firstChildAcl);
        assertThat(secondChild.getAccessControlListByRole("caseworker")).isEmpty();
    }

    @Test
    void shouldRejectComplexAclForUnknownNestedField() {
        CaseFieldDefinition root = complexChild("Root");
        ComplexACL acl = new ComplexACL();
        acl.setRole("caseworker");
        acl.setListElementCode("Missing");
        root.setComplexACLs(List.of(acl));

        org.assertj.core.api.Assertions.assertThatThrownBy(root::propagateACLsToNestedFields)
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("has no nested elements with code Missing");
    }

    @Test
    void shouldApplyComplexAclToNestedDottedPathAndRemoveSiblingAcl() {
        CaseFieldDefinition nestedFirst = complexChild("First");
        CaseFieldDefinition nestedSecond = complexChild("Second");
        CaseFieldDefinition parent = complexChild("Parent");
        parent.getFieldTypeDefinition().setComplexFields(List.of(nestedFirst, nestedSecond));

        CaseFieldDefinition root = complexChild("Root");
        root.getFieldTypeDefinition().setComplexFields(List.of(parent));
        root.setAccessControlLists(List.of(acl("caseworker")));

        ComplexACL nestedAcl = new ComplexACL();
        nestedAcl.setRole("caseworker");
        nestedAcl.setListElementCode("Parent.First");
        nestedAcl.setUpdate(true);
        root.setComplexACLs(List.of(nestedAcl));

        root.propagateACLsToNestedFields();

        assertThat(nestedFirst.getAccessControlLists()).containsExactly(nestedAcl);
        assertThat(nestedSecond.getAccessControlListByRole("caseworker")).isEmpty();
    }

    @Test
    void shouldPropagateAclsThroughCollectionComplexFields() {
        CaseFieldDefinition collectionItem = complexChild("CollectionItem");
        FieldTypeDefinition collectionElementType = new FieldTypeDefinition();
        collectionElementType.setType(FieldTypeDefinition.COMPLEX);
        collectionElementType.setComplexFields(List.of(collectionItem));

        CaseFieldDefinition root = complexChild("Root");
        FieldTypeDefinition collectionType = new FieldTypeDefinition();
        collectionType.setType(FieldTypeDefinition.COLLECTION);
        collectionType.setCollectionFieldTypeDefinition(collectionElementType);
        root.setFieldTypeDefinition(collectionType);
        root.setAccessControlLists(List.of(acl("caseworker")));

        root.propagateACLsToNestedFields();

        assertThat(collectionType.getChildren()).containsExactly(collectionItem);
        assertThat(collectionItem.getAccessControlListByRole("caseworker")).isPresent();
    }

    private AccessControlList acl(String role) {
        AccessControlList acl = new AccessControlList();
        acl.setRole(role);
        acl.setRead(true);
        return acl;
    }

    private CaseFieldDefinition complexChild(String id) {
        CaseFieldDefinition field = new CaseFieldDefinition();
        field.setId(id);
        FieldTypeDefinition type = new FieldTypeDefinition();
        type.setType(FieldTypeDefinition.COMPLEX);
        field.setFieldTypeDefinition(type);
        field.setAccessControlLists(new java.util.ArrayList<>());
        return field;
    }
}
