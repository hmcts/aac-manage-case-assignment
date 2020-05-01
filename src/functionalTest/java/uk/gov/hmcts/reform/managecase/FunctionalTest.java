package uk.gov.hmcts.reform.managecase;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static io.restassured.RestAssured.given;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

public class FunctionalTest {

    @Value("${TEST_URL:http://localhost:4454}")
    private String testUrl;

    @BeforeEach
    public void setup() {
        RestAssured.baseURI = testUrl;
    }

    @Test
    public void info() {
        given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .when()
            .get("/info")
            .then()
            .statusCode(200);
    }
}
