package uk.gov.hmcts.reform.managecase.client.prd;

import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.MockitoAnnotations.openMocks;

class PrdFeignClientTest {

    PrdFeignClient prdFeignClient;

    @Mock
    Request request;

    @BeforeEach
    void setUp() {
        openMocks(this);
        prdFeignClient = new PrdFeignClient(null, null);
    }

    @Test
    @DisplayName("Execute non-null response while status code 200")
    void check_status_200_non_null_body() throws IOException {
        Response defaultResponse = Response.builder()
            .status(200)
            .request(request)
            .body("Successful", StandardCharsets.UTF_8)
            .build();
        Response response = prdFeignClient.checkResponse(defaultResponse);
        assertEquals(200, response.status());
        assertNotNull(response.body());
        assertEquals("Successful", response.body().toString());
    }

    @Test
    @DisplayName("Throw RetryableException while status code 200 and body is null")
    void check_status_200_null_body() {
        Response defaultResponse = Response.builder()
            .status(200)
            .request(request)
            .body((byte[]) null)
            .build();

        assertThrows(RetryableException.class, () -> prdFeignClient.checkResponse(defaultResponse));
    }

    @Test
    @DisplayName("Throw RetryableException while status code 200 and response length is 0")
    void check_status_200_empty_response_length() {
        Response defaultResponse = Response.builder()
            .status(200)
            .request(request)
            .body(new byte[0])
            .build();

        assertThrows(RetryableException.class, () -> prdFeignClient.checkResponse(defaultResponse));
    }

    @Test
    @DisplayName("Execute non-null response while status code 400")
    void check_status_400_non_null_body() throws IOException {
        Response defaultResponse = Response.builder()
            .status(400)
            .request(request)
            .body("Successful", StandardCharsets.UTF_8)
            .build();
        Response response = prdFeignClient.checkResponse(defaultResponse);
        assertEquals(400, response.status());
        assertNotNull(response.body());
        assertEquals("Successful", response.body().toString());
    }

    @Test
    @DisplayName("Execute non-null response while status code 400")
    void check_status_400_null_body() throws IOException {
        Response defaultResponse = Response.builder()
            .status(400)
            .request(request)
            .body((byte[]) null)
            .build();
        Response response = prdFeignClient.checkResponse(defaultResponse);
        assertEquals(400, response.status());
        assertNull(response.body());
    }
}
