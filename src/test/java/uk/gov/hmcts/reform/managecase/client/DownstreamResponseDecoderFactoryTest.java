package uk.gov.hmcts.reform.managecase.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import feign.codec.Decoder;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.config.JacksonObjectMapperConfig;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DownstreamResponseDecoderFactoryTest {

    private final ObjectMapper objectMapper = new JacksonObjectMapperConfig().defaultObjectMapper();

    @Test
    void shouldIgnoreAdditiveFieldsOnDownstreamProviderResponses() throws Exception {
        Decoder decoder = DownstreamResponseDecoderFactory.tolerantJsonDecoder(objectMapper);

        CaseDetails caseDetails = (CaseDetails) decoder.decode(jsonResponse("""
            {
              "id": "1588234985453946",
              "version": 4,
              "case_type": "FT_MasterCaseType",
              "data": {},
              "future_data_store_field": "provider-added-value"
            }
            """), CaseDetails.class);

        assertThat(caseDetails.getId()).isEqualTo("1588234985453946");
        assertThat(caseDetails.getVersion()).isEqualTo(4);
        assertThat(caseDetails.getCaseTypeId()).isEqualTo("FT_MasterCaseType");
    }

    private Response jsonResponse(String body) {
        Request request = Request.create(
            Request.HttpMethod.GET,
            "http://ccd-data-store-api/cases/1588234985453946",
            Map.of(),
            null,
            StandardCharsets.UTF_8,
            null
        );

        return Response.builder()
            .request(request)
            .status(200)
            .headers(Map.of("Content-Type", java.util.List.of("application/json")))
            .body(body, StandardCharsets.UTF_8)
            .build();
    }
}
