package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.config.JacksonObjectMapperConfig;

import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class CaseViewJsonTest {

    private final ObjectMapper mapper = new JacksonObjectMapperConfig().defaultObjectMapper();

    @Test
    void shouldUseApiPropertyNamesForCaseView() throws Exception {
        CaseView view = new CaseView();
        view.setCaseId("123");
        view.setActionableEvents(new CaseViewActionableEvent[0]);
        view.setEvents(new CaseViewEvent[0]);

        var json = mapper.readTree(mapper.writeValueAsString(view));

        assertThat(json.get("case_id").asText()).isEqualTo("123");
        assertThat(json.has("triggers")).isTrue();
        assertThat(json.has("events")).isTrue();
    }

    @Test
    void shouldReadCaseViewResourcePropertyNames() throws Exception {
        String json = "{\"case_id\":\"456\",\"triggers\":[],\"events\":[]}";

        CaseViewResource resource = mapper.readValue(json, CaseViewResource.class);

        assertThat(resource.getReference()).isEqualTo("456");
        assertThat(resource.getCaseViewActionableEvents()).isEmpty();
        assertThat(resource.getCaseViewEvents()).isEmpty();
    }

    @Test
    void shouldUseSnakeCaseForViewTabAndStateProperties() throws Exception {
        CaseViewTab tab = new CaseViewTab("tab", "Details", 1, new CaseViewField[0], "show", "caseworker");
        CaseStateDefinition state = new CaseStateDefinition();
        state.setId("Submitted");
        state.setDisplayOrder(2);
        state.setTitleDisplay("Submitted");

        var tabJson = mapper.readTree(mapper.writeValueAsString(tab));
        var stateJson = mapper.readTree(mapper.writeValueAsString(state));

        assertThat(tabJson.get("show_condition").asText()).isEqualTo("show");
        assertThat(stateJson.get("order").asInt()).isEqualTo(2);
        assertThat(stateJson.get("title_display").asText()).isEqualTo("Submitted");
        assertThat(stateJson.has("ANY")).isFalse();
    }

    @Test
    void shouldMapComplexEventFieldProperties() throws Exception {
        CaseEventFieldComplexDefinition definition = CaseEventFieldComplexDefinition.builder()
            .reference("AddressLine1")
            .order(1)
            .displayContextParameter("#TABLE(Address)")
            .defaultValue("Unknown")
            .retainHiddenValue(true)
            .build();

        var json = mapper.readTree(mapper.writeValueAsString(definition));

        assertThat(json.get("reference").asText()).isEqualTo("AddressLine1");
        assertThat(json.get("displayContextParameter").asText()).isEqualTo("#TABLE(Address)");
        assertThat(json.get("retainHiddenValue").asBoolean()).isTrue();
    }

    @Test
    void shouldMapAuditEventToCaseViewEvent() {
        var created = java.time.LocalDateTime.of(2026, Month.SEPTEMBER, 18, 12, 0);
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setId(42L);
        auditEvent.setEventId("Submit");
        auditEvent.setEventName("Submit case");
        auditEvent.setUserId("user-1");
        auditEvent.setUserFirstName("Ada");
        auditEvent.setUserLastName("Lovelace");
        auditEvent.setDescription("Submitted");
        auditEvent.setCreatedDate(created);
        auditEvent.setStateId("submitted");
        auditEvent.setStateName("Submitted");

        CaseViewEvent result = CaseViewEvent.createFrom(auditEvent);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getEventId()).isEqualTo("Submit");
        assertThat(result.getEventName()).isEqualTo("Submit case");
        assertThat(result.getUserFirstName()).isEqualTo("Ada");
        assertThat(result.getUserLastName()).isEqualTo("Lovelace");
        assertThat(result.getComment()).isEqualTo("Submitted");
        assertThat(result.getTimestamp()).isEqualTo(created);
        assertThat(result.getStateId()).isEqualTo("submitted");
    }
}
