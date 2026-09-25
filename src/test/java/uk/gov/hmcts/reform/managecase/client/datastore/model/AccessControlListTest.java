package uk.gov.hmcts.reform.managecase.client.datastore.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessControlListTest {

    @Test
    void shouldDuplicateAclWithoutSharingMutableState() {
        AccessControlList acl = new AccessControlList();
        acl.setRole("caseworker");
        acl.setCreate(true);
        acl.setRead(true);
        acl.setUpdate(false);
        acl.setDelete(true);

        AccessControlList duplicate = acl.duplicate();

        assertThat(duplicate).usingRecursiveComparison().isEqualTo(acl);
        assertThat(duplicate).isNotSameAs(acl);
        assertThat(acl).hasToString("ACL{role='caseworker', crud=CRD}");
    }
}
