package uk.gov.hmcts.reform.managecase.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.domain.DynamicList;
import uk.gov.hmcts.reform.managecase.domain.DynamicListElement;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
                () -> assertEquals("[Creator]", dynamicList.get(DynamicList.VALUE)
                    .get(DynamicListElement.CODE).textValue()),
                () -> assertEquals("Creator", dynamicList.get(DynamicList.VALUE)
                    .get(DynamicListElement.LABEL).textValue()),
                () -> assertEquals("[Creator]", dynamicList.get(DynamicList.LIST_ITEMS).get(0)
                                       .get(DynamicListElement.CODE).textValue()),
                () -> assertEquals("Creator", dynamicList.get(DynamicList.LIST_ITEMS).get(0)
                    .get(DynamicListElement.LABEL).textValue()),
                () -> assertEquals("[Debtor]", dynamicList.get(DynamicList.LIST_ITEMS).get(1)
                    .get(DynamicListElement.CODE).textValue()),
                () -> assertEquals("Debtor", dynamicList.get(DynamicList.LIST_ITEMS).get(1)
                    .get(DynamicListElement.LABEL).textValue()),
                () -> assertEquals("[Litigant]", dynamicList.get(DynamicList.LIST_ITEMS).get(2)
                    .get(DynamicListElement.CODE).textValue()),
                () -> assertEquals("Litigant", dynamicList.get(DynamicList.LIST_ITEMS)
                    .get(2).get(DynamicListElement.LABEL).textValue())
            );
        }
    }
}
