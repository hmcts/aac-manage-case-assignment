package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

class CaseDetailsTest {

    private ObjectMapper mapper = new ObjectMapper()
        .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));

    @Test
    void givenTwoJsonFormatsForCaseIdWhenDeserialisedThenCaseDetailsObjectsCreated() throws Exception {

        CaseDetails caseDetails = mapper.readValue("{\n"
            + "  \"id\": \"12345\",\n"
            + "  \"jurisdiction\": \"CMC\"\n"
            + "}", CaseDetails.class);

        assertEquals("12345", caseDetails.getId());
        assertEquals("CMC", caseDetails.getJurisdiction());

        caseDetails = mapper.readValue("{\n"
            + "  \"reference\": \"12345\",\n"
            + "  \"jurisdiction\": \"CMC\"\n"
            + "}", CaseDetails.class);

        assertEquals("12345", caseDetails.getId());
        assertEquals("CMC", caseDetails.getJurisdiction());
    }

    @Test
    void shouldSerialiseToCaseIdAlways() throws Exception {

        CaseDetails caseDetails = CaseDetails.builder()
            .id("12345")
            .jurisdiction("CMC")
            .build();

        String json = mapper.writeValueAsString(caseDetails);

        assertEquals("{\"jurisdiction\":\"CMC\",\"id\":\"12345\"}", json, JSONCompareMode.LENIENT);
    }
}