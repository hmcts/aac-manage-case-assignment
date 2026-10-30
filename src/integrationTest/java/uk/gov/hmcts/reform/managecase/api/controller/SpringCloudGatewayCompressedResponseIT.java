package uk.gov.hmcts.reform.managecase.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.managecase.Application;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

@SpringBootTest(
    classes = Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cache.type=none",
        "CCD_DATA_STORE_API_BASE_URL=http://localhost:${wiremock.server.port}",
        "IDAM_API_URL=http://localhost:${wiremock.server.port}",
        "S2S_URL=http://localhost:${wiremock.server.port}/s2s",
        "PRD_HOST=http://localhost:${wiremock.server.port}",
        "IDAM_OIDC_URL=http://localhost:${wiremock.server.port}"
    }
)
@AutoConfigureWireMock(port = 0, stubs = "classpath:/wiremock-stubs")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SpringCloudGatewayCompressedResponseIT {

    private static final String SERVICE_NAME = "xui_webapp";
    private static final String REQUEST_BODY = "{\"query\":{\"match_all\":{}},\"size\":50}";
    private static final String COMPLETION_SENTINEL = "CCD-8333-GATEWAY-RESPONSE-COMPLETE";

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUpJwtDecoder() {
        Jwt jwt = Jwt.withTokenValue("user-token")
            .header("alg", "none")
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(Instant.now().plusSeconds(3600))
            .subject("445")
            .build();
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);
    }

    @Test
    void shouldReturnCompleteDecompressedResponseWithoutCompressedEntityHeaders() throws Exception {
        String responseBody = "{\"cases\":[{\"description\":\""
            + "case-description-".repeat(2_000)
            + COMPLETION_SENTINEL
            + "\"}]}";
        byte[] compressedBody = gzip(responseBody);

        stubFor(WireMock.post(urlEqualTo("/searchCases?ctid=CT_Compressed"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withHeader(HttpHeaders.CONTENT_ENCODING, "gzip")
                .withHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(compressedBody.length))
                .withHeader("Content-MD5", "compressed-entity-digest")
                .withBody(compressedBody)));

        HttpResponse<byte[]> response = sendGatewayRequest("CT_Compressed");
        String responseText = new String(response.body(), StandardCharsets.UTF_8);

        assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(200);
            softly.assertThat(response.headers().firstValue(HttpHeaders.CONTENT_ENCODING))
                .as("Content-Encoding after the entity has been decompressed")
                .isEmpty();
            softly.assertThat(response.headers().firstValue(HttpHeaders.CONTENT_LENGTH))
                .as("Content-Length for the decompressed response")
                .isEmpty();
            softly.assertThat(response.headers().firstValue("Content-MD5"))
                .as("Content-MD5 for the compressed entity")
                .isEmpty();
            softly.assertThat(response.body().length)
                .as("complete decompressed response length")
                .isEqualTo(responseBody.getBytes(StandardCharsets.UTF_8).length);
            softly.assertThat(responseText).contains(COMPLETION_SENTINEL);
            softly.assertThatCode(() -> objectMapper.readTree(responseText))
                .as("complete response is valid JSON")
                .doesNotThrowAnyException();
        });

        verify(postRequestedFor(urlEqualTo("/searchCases?ctid=CT_Compressed"))
            .withHeader(HttpHeaders.ACCEPT_ENCODING, containing("gzip")));
    }

    @Test
    void shouldNotAlterAnUncompressedGatewayResponseBody() throws Exception {
        String responseBody = "{\"cases\":[{\"description\":\"uncompressed-"
            + COMPLETION_SENTINEL
            + "\"}]}";
        stubFor(WireMock.post(urlEqualTo("/searchCases?ctid=CT_Uncompressed"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(responseBody)));

        HttpResponse<byte[]> response = sendGatewayRequest("CT_Uncompressed");
        String responseText = new String(response.body(), StandardCharsets.UTF_8);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(responseText).isEqualTo(responseBody).contains(COMPLETION_SENTINEL);
        assertThat(objectMapper.readTree(responseText).path("cases").size()).isEqualTo(1);
    }

    private HttpResponse<byte[]> sendGatewayRequest(String caseTypeId) throws Exception {
        String serviceToken = Jwts.builder()
            .subject(SERVICE_NAME)
            .issuedAt(new Date())
            .signWith(Jwts.SIG.HS256.key().build())
            .compact();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/ccd/searchCases?ctid=" + caseTypeId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
            .header(SERVICE_AUTHORIZATION, "Bearer " + serviceToken)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(REQUEST_BODY))
            .build();

        return HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build()
            .send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static byte[] gzip(String content) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return output.toByteArray();
    }
}
