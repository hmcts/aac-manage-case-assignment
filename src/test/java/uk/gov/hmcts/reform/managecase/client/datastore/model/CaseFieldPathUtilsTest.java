package uk.gov.hmcts.reform.managecase.client.datastore.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;

@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
class CaseFieldPathUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    class GetNestedCaseFieldByPathTest {

        @Test
        void shouldFindTopLevelField() throws JsonProcessingException {
            Map<String, JsonNode> caseData = createCaseData();
            JsonNode result = CaseFieldPathUtils.getNestedCaseFieldByPath(caseData, "TextField");

            assertAll(
                () -> assertThat(result.isTextual(), is(true)),
                () -> assertThat(result.asText(), is("TextValue"))
            );
        }

        @Test
        void shouldFindNestedField() throws JsonProcessingException {
            Map<String, JsonNode> caseData = createCaseData();
            JsonNode result = CaseFieldPathUtils.getNestedCaseFieldByPath(caseData,
                "ComplexField.ComplexNestedField.NestedNumberField");

            assertAll(
                () -> assertThat(result.isTextual(), is(true)),
                () -> assertThat(result.asText(), is("67890"))
            );
        }

        @Test
        void shouldFindNullNode() throws JsonProcessingException {
            Map<String, JsonNode> caseData = createCaseData();
            JsonNode result = CaseFieldPathUtils.getNestedCaseFieldByPath(caseData,"TextAreaField");

            assertAll(
                () -> assertThat(result.isNull(), is(true))
            );
        }

        @Test
        void shouldReturnNullForNonExistingNode() throws JsonProcessingException {
            Map<String, JsonNode> caseData = createCaseData();
            JsonNode result = CaseFieldPathUtils.getNestedCaseFieldByPath(caseData,"NonExisting");

            assertAll(
                () -> assertThat(result, is(nullValue()))
            );
        }

        private Map<String, JsonNode> createCaseData() throws JsonProcessingException {
            return objectMapper.readValue(caseDataString(), new TypeReference<Map<String, JsonNode>>() {});
        }

        private String caseDataString() {
            return "{\n"
                + "    \"DateField\": \"1985-07-25\",\n"
                + "    \"TextField\": \"TextValue\",\n"
                + "    \"EmailField\": \"test@email.com\",\n"
                + "    \"NumberField\": \"12345\",\n"
                + "    \"ComplexField\": {\n"
                + "        \"ComplexNestedField\": {\n"
                + "            \"NestedNumberField\": \"67890\",\n"
                + "            \"NestedCollectionTextField\": []\n"
                + "        },\n"
                + "        \"ComplexTextField\": \"ComplexTextValue\"\n"
                + "    },\n"
                + "    \"PhoneUKField\": \"01234 567890\",\n"
                + "    \"YesOrNoField\": \"No\",\n"
                + "    \"DateTimeField\": \"2020-12-15T12:30:15.000\",\n"
                + "    \"MoneyGBPField\": \"25000\",\n"
                + "    \"TextAreaField\": null,\n"
                + "    \"AddressUKField\": {\n"
                + "        \"County\": \"CountValue\",\n"
                + "        \"Country\": \"CountryValue\",\n"
                + "        \"PostCode\": \"PST CDE\",\n"
                + "        \"PostTown\": \"TownValue\",\n"
                + "        \"AddressLine1\": \"BuildingValue\",\n"
                + "        \"AddressLine2\": \"AddressLine2Value\",\n"
                + "        \"AddressLine3\": \"AddressLine3Value\"\n"
                + "    }\n"
                + "}";
        }
    }
}