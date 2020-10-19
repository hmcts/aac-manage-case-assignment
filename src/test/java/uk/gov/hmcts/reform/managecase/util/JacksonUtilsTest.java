package uk.gov.hmcts.reform.managecase.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JacksonUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JacksonUtils jacksonUtils;

    @BeforeEach
    void setUp() {
        jacksonUtils = new JacksonUtils(objectMapper);
    }

    @Test
    void testNullifyObjectNode() throws JsonProcessingException {
        JsonNode node = objectMapper.readTree("{\n"
            + "        \"Reason\": \"reason\",\n"
            + "        \"CaseRoleId\": null,\n"
            + "        \"NotesReason\": \"a\",\n"
            + "        \"ApprovalStatus\": null,\n"
            + "        \"RequestTimestamp\": null,\n"
            + "        \"OrganisationToAdd\": {\n"
            + "            \"OrganisationID\": \"\",\n"
            + "            \"OrganisationName\": null\n"
            + "        },\n"
            + "        \"OrganisationToRemove\": {\n"
            + "            \"OrganisationID\": \"OrgID\",\n"
            + "            \"OrganisationName\": \"OrgName\"\n"
            + "        },\n"
            + "        \"ApprovalRejectionTimestamp\": null\n"
            + "    }");

        jacksonUtils.nullifyObjectNode((ObjectNode) node);

        System.out.println("test");
    }
}