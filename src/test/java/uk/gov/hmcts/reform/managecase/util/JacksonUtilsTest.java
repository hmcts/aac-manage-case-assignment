package uk.gov.hmcts.reform.managecase.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import uk.gov.hmcts.reform.managecase.domain.DynamicList;
import uk.gov.hmcts.reform.managecase.domain.DynamicListElement;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Maps.newHashMap;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;

class JacksonUtilsTest {

    public static final String COR_STRING = "{\n"
        + "    \"CaseRoleId\": {\n"
        + "      \"value\": {\n"
        + "        \"code\": \"[LEGALREPRESENTATIVE]\",\n"
        + "        \"label\": \"Legal Representative\"\n"
        + "      },\n"
        + "      \"list_items\": [\n"
        + "        {\n"
        + "          \"code\": \"[LEGALREPRESENTATIVE]\",\n"
        + "          \"label\": \"Legal Representative\"\n"
        + "        }\n"
        + "      ]\n"
        + "    },\n"
        + "    \"ApprovalStatus\": \"1\",\n"
        + "    \"NotesReason\": null,\n"
        + "    \"RequestTimestamp\": \"2021-03-03T15:43:55.779895\",\n"
        + "    \"OrganisationToAdd\": {\n"
        + "      \"OrganisationID\": \"8P7DJ0K\"\n"
        + "    },\n"
        + "    \"OrganisationToRemove\": {\n"
        + "      \"OrganisationID\": \"7V6U9KC\"\n"
        + "    }\n"
        + "  }";
    public static final String NULL_COR_NODE = "{\n"
        + "    \"CaseRoleId\": {\n"
        + "      \"value\": {\n"
        + "        \"code\": \"[LEGALREPRESENTATIVE]\",\n"
        + "        \"label\": \"Legal Representative\"\n"
        + "      },\n"
        + "      \"list_items\": [\n"
        + "        {\n"
        + "          \"code\": \"[LEGALREPRESENTATIVE]\",\n"
        + "          \"label\": \"Legal Representative\"\n"
        + "        }\n"
        + "      ]\n"
        + "    },\n"
        + "    \"ApprovalStatus\": null,\n"
        + "    \"RequestTimestamp\": null,\n"
        + "    \"OrganisationToAdd\": {\n"
        + "      \"OrganisationID\": null\n"
        + "    },\n"
        + "    \"OrganisationToRemove\": {\n"
        + "      \"OrganisationID\": null\n"
        + "    }\n"
        + "  }";

    public static final String CHANGE_ORGANISATION_REQUEST_FIELD = "changeOrganisationRequestField";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JacksonUtils jacksonUtils;

    @BeforeEach
    void setUp() {
        jacksonUtils = new JacksonUtils(objectMapper);
    }

    @Test
    void shouldNullifyObjectNode() throws JsonProcessingException {
        JsonNode node = objectMapper.readTree("{\n"
            + "    \"Reason\": \"reason\",\n"
            + "    \"CaseRoleId\": {\n"
            + "        \"value\": {\n"
            + "            \"code\": \"[Claimant]\",\n"
            + "            \"label\": \"Claimant\"\n"
            + "        },\n"
            + "        \"list_items\": [\n"
            + "            {\n"
            + "                \"code\": \"[Defendant]\",\n"
            + "                \"label\": \"Defendant\"\n"
            + "            },\n"
            + "            {\n"
            + "                \"code\": \"[Claimant]\",\n"
            + "                \"label\": \"Claimant\"\n"
            + "            }\n"
            + "        ]\n"
            + "    },\n"
            + "    \"NotesReason\": \"reason\",\n"
            + "    \"ApprovalStatus\": \"status\",\n"
            + "    \"RequestTimestamp\": \"date1\",\n"
            + "    \"OrganisationToAdd\": {\n"
            + "        \"OrganisationID\": \"OrgId1\",\n"
            + "        \"OrganisationName\": \"OrgName1\"\n"
            + "    },\n"
            + "    \"OrganisationToRemove\": {\n"
            + "        \"OrganisationID\": \"OrgId2\",\n"
            + "        \"OrganisationName\": \"OrgName2\"\n"
            + "    },\n"
            + "    \"ApprovalRejectionTimestamp\": \"date2\"\n"
            + "}");

        jacksonUtils.nullifyObjectNode((ObjectNode) node, "CaseRoleId");

        assertThat(node).hasToString("{\"Reason\":null,\"CaseRoleId\":null,\"NotesReason\":null,"
            + "\"ApprovalStatus\":null,\"RequestTimestamp\":null,\"OrganisationToAdd\":{\"OrganisationID\":null,"
            + "\"OrganisationName\":null},\"OrganisationToRemove\":{\"OrganisationID\":null,"
            + "\"OrganisationName\":null},\"ApprovalRejectionTimestamp\":null}");
    }

    @Test
    void shouldMergeWhenCaseDataDoesNotHaveCorNode() throws JsonProcessingException {

        Map<String, JsonNode> cordData = newHashMap(
            CHANGE_ORGANISATION_REQUEST_FIELD,
            objectMapper.readTree(COR_STRING)
        );

        JsonNode caseNodes = objectMapper.readTree("{\"name\": \"Sateesh\"}");
        Map<String, JsonNode> caseData = newHashMap("person", caseNodes);

        jacksonUtils.merge(cordData, caseData);

        JSONAssert.assertEquals(caseData.get(CHANGE_ORGANISATION_REQUEST_FIELD).toString(),
                                COR_STRING, LENIENT);
    }

    @Test
    void shouldMergeWhenCaseDataHasNullCorNodes() throws JsonProcessingException {
        Map<String, JsonNode> cordData = newHashMap(
            CHANGE_ORGANISATION_REQUEST_FIELD,
            objectMapper.readTree(COR_STRING)
        );

        JsonNode caseNodes = objectMapper.readTree(NULL_COR_NODE);
        Map<String, JsonNode> caseData = newHashMap(CHANGE_ORGANISATION_REQUEST_FIELD, caseNodes);

        jacksonUtils.merge(cordData, caseData);

        JSONAssert.assertEquals(caseData.get(CHANGE_ORGANISATION_REQUEST_FIELD).toString(),
                                COR_STRING, LENIENT);
    }

    @Nested
    @DisplayName("JacksonUtils")
    class JacksonUtilsDynamicList {

        @BeforeEach
        void setUp() {
            jacksonUtils = new JacksonUtils(new ObjectMapper());
        }

        @Test
        @DisplayName("")
        public void createDynamicList() {

            DynamicListElement element1 = DynamicListElement.builder()
                .code("[Creator]")
                .label("Creator")
                .build();
            DynamicListElement element2 = DynamicListElement.builder()
                .code("[Debtor]")
                .label("Debtor")
                .build();
            DynamicListElement element3 = DynamicListElement.builder()
                .code("[Litigant]")
                .label("Litigant")
                .build();

            ObjectNode dynamicList = jacksonUtils.createDynamicList(
                element1,
                Arrays.asList(element1, element2, element3)
            );

            assertAll(
                () -> assertEquals("[Creator]", dynamicList.get(DynamicList.VALUE_PARAM)
                    .get(DynamicListElement.CODE_PARAM).textValue()),
                () -> assertEquals("Creator", dynamicList.get(DynamicList.VALUE_PARAM)
                    .get(DynamicListElement.LABEL_PARAM).textValue()),
                () -> assertEquals("[Creator]", dynamicList.get(DynamicList.LIST_ITEMS).get(0)
                                       .get(DynamicListElement.CODE_PARAM).textValue()),
                () -> assertEquals("Creator", dynamicList.get(DynamicList.LIST_ITEMS).get(0)
                    .get(DynamicListElement.LABEL_PARAM).textValue()),
                () -> assertEquals("[Debtor]", dynamicList.get(DynamicList.LIST_ITEMS).get(1)
                    .get(DynamicListElement.CODE_PARAM).textValue()),
                () -> assertEquals("Debtor", dynamicList.get(DynamicList.LIST_ITEMS).get(1)
                    .get(DynamicListElement.LABEL_PARAM).textValue()),
                () -> assertEquals("[Litigant]", dynamicList.get(DynamicList.LIST_ITEMS).get(2)
                    .get(DynamicListElement.CODE_PARAM).textValue()),
                () -> assertEquals("Litigant", dynamicList.get(DynamicList.LIST_ITEMS)
                    .get(2).get(DynamicListElement.LABEL_PARAM).textValue())
            );
        }
    }
}
