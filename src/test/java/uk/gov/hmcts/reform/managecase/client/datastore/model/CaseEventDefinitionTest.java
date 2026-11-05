package uk.gov.hmcts.reform.managecase.client.datastore.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CaseEventDefinitionTest {

    @Test
    void shouldFindCaseEventFieldById() {
        CaseEventFieldDefinition field = new CaseEventFieldDefinition();
        field.setCaseFieldId("Surname");

        CaseEventDefinition event = new CaseEventDefinition();
        event.setCaseFields(java.util.List.of(field));

        assertThat(event.getCaseEventField("Surname")).containsSame(field);
        assertThat(event.getCaseEventField("Unknown")).isEmpty();
    }
}
