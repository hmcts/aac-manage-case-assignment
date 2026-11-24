package uk.gov.hmcts.reform.managecase.client.definitionstore.model;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.client.datastore.model.AccessControlList;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FieldTypeTest {

    @Test
    void shouldClearAclsWhenComplexFieldsAreAssigned() {
        CaseField complexField = new CaseField();
        complexField.setAcls(List.of(new AccessControlList()));

        FieldType fieldType = FieldType.builder().build();
        fieldType.setComplexFields(List.of(complexField));

        assertThat(fieldType.getComplexFields()).containsExactly(complexField);
        assertThat(complexField.getAcls()).isNull();
    }

    @Test
    void shouldLeaveComplexFieldsUnsetWhenNullIsAssigned() {
        FieldType fieldType = FieldType.builder().build();

        fieldType.setComplexFields(null);

        assertThat(fieldType.getComplexFields()).isNull();
    }
}
