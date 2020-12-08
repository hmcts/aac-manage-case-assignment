package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION_TO_REMOVE;

class CaseDetailsTest {
    public static final String COR_FIELD_NAME = "ChangeOrganisationRequestField2";

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should find ChangeOrganisationRequest field name")
    void shouldFindChangeOrganisationRequestFieldName() throws Exception {

        JsonNode corJsonNode = objectMapper.readTree(objectMapper.writeValueAsString(
            ChangeOrganisationRequest.builder().build()));

        Map<String, JsonNode> data = dataWithChangeOrganisationRequest(corJsonNode);
        CaseDetails caseDetails = CaseDetails.builder().data(data).build();

        Optional<String> result = caseDetails.findChangeOrganisationRequestFieldName();

        assertTrue(result.isPresent(), format("%s should be present.", COR_FIELD_NAME));
        assertEquals(COR_FIELD_NAME, result.get(), format("Field name should be %s", COR_FIELD_NAME));
    }

    @Test
    @DisplayName("Should return Optional.empty() when no ChangeOrganisationRequest field name")
    void shouldReturnEmptyWhenNoChangeOrganisationRequestFieldName() throws Exception {

        Map<String, JsonNode> data = dataWithoutChangeOrganisationRequest();

        CaseDetails caseDetails = CaseDetails.builder().data(data).build();

        Optional<String> result = caseDetails.findChangeOrganisationRequestFieldName();

        assertTrue(result.isEmpty(), "No field should be found.");
    }

    private Map<String, JsonNode> dataWithChangeOrganisationRequest(JsonNode corJsonNode) {
        Map<String, JsonNode> data = dataWithoutChangeOrganisationRequest();
        data.put(COR_FIELD_NAME, corJsonNode);
        return data;
    }

    private Map<String, JsonNode> dataWithoutChangeOrganisationRequest() {
        Map<String, JsonNode> data = new ConcurrentHashMap<>();
        data.put("Name", TextNode.valueOf("Test"));
        data.put("FakeCOR", JSON_NODE_FACTORY.objectNode().put(ORGANISATION_TO_REMOVE, "test value"));
        return data;
    }
}
