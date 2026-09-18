package uk.gov.hmcts.reform.managecase.client.datastore.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseTypeDefinitionTest {

    @Test
    void shouldFindEventsAndClassifyFields() {
        CaseEventDefinition event = new CaseEventDefinition();
        event.setId("Submit");
        event.setCanSaveDraft(true);

        CaseFieldDefinition field = new CaseFieldDefinition();
        field.setId("SensitiveField");
        field.setSecurityLabel("RESTRICTED");

        JurisdictionDefinition jurisdiction = new JurisdictionDefinition();
        jurisdiction.setId("PROBATE");

        CaseTypeDefinition caseType = new CaseTypeDefinition();
        caseType.setName("Example");
        caseType.setJurisdictionDefinition(jurisdiction);
        caseType.setEvents(List.of(event));
        caseType.setCaseFieldDefinitions(List.of(field));

        assertThat(caseType.getJurisdictionId()).isEqualTo("PROBATE");
        assertThat(caseType.hasDraftEnabledEvent()).isTrue();
        assertThat(caseType.hasEventId("submit")).isTrue();
        assertThat(caseType.findCaseEvent("SUBMIT")).containsSame(event);
        assertThat(caseType.getClassificationForField("SensitiveField"))
            .isEqualTo(SecurityClassification.RESTRICTED);
    }

    @Test
    void shouldRejectUnknownFieldClassification() {
        CaseTypeDefinition caseType = new CaseTypeDefinition();
        caseType.setName("Example");

        assertThatThrownBy(() -> caseType.getClassificationForField("Unknown"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("CaseFieldId Unknown not found in CaseType Example");
    }
}
