package uk.gov.hmcts.reform.managecase.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JacksonUtils jacksonUtils;

    @BeforeEach
    void setUp() {
        jacksonUtils = new JacksonUtils(objectMapper);
    }

    @Test
    void shouldNullifyObjectNode() throws JsonProcessingException {
        JsonNode node = objectMapper.readTree("{\n"
            + "        \"Reason\": \"reason\",\n"
            + "        \"CaseRoleId\": \"case role\",\n"
            + "        \"NotesReason\": \"reason\",\n"
            + "        \"ApprovalStatus\": \"status\",\n"
            + "        \"RequestTimestamp\": \"date1\",\n"
            + "        \"OrganisationToAdd\": {\n"
            + "            \"OrganisationID\": \"OrgId1\",\n"
            + "            \"OrganisationName\": \"OrgName1\"\n"
            + "        },\n"
            + "        \"OrganisationToRemove\": {\n"
            + "            \"OrganisationID\": \"OrgId2\",\n"
            + "            \"OrganisationName\": \"OrgName2\"\n"
            + "        },\n"
            + "        \"ApprovalRejectionTimestamp\": \"date2\"\n"
            + "    }");

        jacksonUtils.nullifyObjectNode((ObjectNode) node);

        assertThat(node).hasToString("{\"Reason\":null,\"CaseRoleId\":null,\"NotesReason\":null,"
            + "\"ApprovalStatus\":null,\"RequestTimestamp\":null,\"OrganisationToAdd\":{\"OrganisationID\":null,"
            + "\"OrganisationName\":null},\"OrganisationToRemove\":{\"OrganisationID\":null,"
            + "\"OrganisationName\":null},\"ApprovalRejectionTimestamp\":null}");
    }
}
