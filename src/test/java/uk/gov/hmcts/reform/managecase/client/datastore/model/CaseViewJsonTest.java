package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.config.JacksonObjectMapperConfig;

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
}
