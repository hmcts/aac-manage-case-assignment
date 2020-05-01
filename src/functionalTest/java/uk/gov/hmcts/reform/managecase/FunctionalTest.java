package uk.gov.hmcts.reform.managecase;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

public class FunctionalTest {

    @BeforeEach
    public void setUp() {
        RestAssured.baseURI = System.getenv("TEST_URL");
    }

    @Test
    public void info() {
        Response response = given()
            .relaxedHTTPSValidation()
            .header(CONTENT_TYPE, "application/json")
            .when()
            .get("/info");
        assertThat(response.statusCode()).isEqualTo(200);
    }
}
