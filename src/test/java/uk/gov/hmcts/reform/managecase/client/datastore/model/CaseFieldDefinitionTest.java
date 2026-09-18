package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class CaseFieldDefinitionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerialiseFormattedValue() throws JsonProcessingException {
        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        JsonNode formattedValue = objectMapper.readTree("{\"code\":\"A\",\"label\":\"Accepted\"}");
        caseFieldDefinition.setFormattedValue(formattedValue);

        String json = objectMapper.writeValueAsString(caseFieldDefinition);
        CaseFieldDefinition result = objectMapper.readValue(json, CaseFieldDefinition.class);

        assertThat(objectMapper.valueToTree(result.getFormattedValue()), is(formattedValue));
    }
}
