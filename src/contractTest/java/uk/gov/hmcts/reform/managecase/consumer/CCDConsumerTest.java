package uk.gov.hmcts.reform.managecase.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.common.collect.ImmutableMap;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
public class CCDConsumerTest {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private static final String IDAM_OAUTH2_TOKEN = "pact-test-idam-token";
    private static final String SERVICE_AUTHORIZATION_TOKEN = "pact-test-s2s-token";

    static Map<String, String> headers = ImmutableMap.of(
        HttpHeaders.AUTHORIZATION, IDAM_OAUTH2_TOKEN,
        SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN
    );

    private static final String CASE_ASSIGNMENT_REQUEST = "{"
        + "\"case_users\": ["
                + " {\"organisation_id\": \"Test_Org\", \"case_id\": \"1588234985453946\", \"user_id\": \"abc123\","
                + " \"case_role\": \"[Collaborator]\" }"
            + "]"
        + "}";

    @Pact(provider = "ccd", consumer = "mca_consumer")
    public RequestResponsePact getCaseAssignments(PactDslWithProvider builder) throws Exception {
        return builder
            .given("CCD successfully return case assignments")
            .uponReceiving("Request to get case assignments")
                .path("/case-users")
                .method(HttpMethod.GET.toString())
                .matchQuery("case_ids", "^\\d{16,16}$", "1588234985453946")
                .matchQuery("user_ids", "[a-zA-Z0-9-]+", "33dff5a7-3b6f-45f1-b5e7-5f9be1ede355")
                .headers(headers)
            .willRespondWith()
                .status(HttpStatus.OK.value())
                .body(
                    new PactDslJsonBody()
                        .minArrayLike("case_users", 1, 1)
                        .stringType("case_id", "1588234985453946")
                        .stringType("user_id", "33dff5a7-3b6f-45f1-b5e7-5f9be1ede355")
                        .stringType("case_role", "[Collaborator]")
                        .closeArray()
                )
            .toPact();
    }

    @Pact(provider = "ccd", consumer = "mca_consumer")
    public RequestResponsePact getCaseAssignmentsWhenCaseIdsPassed(PactDslWithProvider builder) throws Exception {
        return builder
            .given("CCD successfully return case assignments")
            .uponReceiving("Request to get case assignments")
            .path("/case-users")
            .method(HttpMethod.GET.toString())
            .headers(headers)
            .willRespondWith()
            .status(HttpStatus.BAD_REQUEST.value())
            .body(
                new PactDslJsonBody()
                    .stringValue("message", "Case ID list is empty")
            )
            .toPact();
    }

    @Pact(provider = "ccd", consumer = "mca_consumer")
    public RequestResponsePact addCaseAssignments(PactDslWithProvider builder) throws Exception {
        return builder
            .given("CCD successfully add case assignments")
            .uponReceiving("Request to add case assignments")
            .path("/case-users")
            .method(HttpMethod.POST.toString())
            .body(CASE_ASSIGNMENT_REQUEST, ContentType.APPLICATION_JSON)
            .headers(headers)
            .willRespondWith()
            .status(HttpStatus.CREATED.value())
            .body(
                new PactDslJsonBody()
                    .stringValue("message", "Case-User-Role assignments created successfully")
            )
            .toPact();
    }

    @Pact(provider = "ccd", consumer = "mca_consumer")
    public RequestResponsePact removeCaseAssignments(PactDslWithProvider builder) throws Exception {
        return builder
            .given("CCD successfully remove case assignments")
            .uponReceiving("Request to remove case assignments")
            .path("/case-users")
            .method(HttpMethod.DELETE.toString())
            .body(CASE_ASSIGNMENT_REQUEST, ContentType.APPLICATION_JSON)
            .headers(headers)
            .willRespondWith()
            .status(HttpStatus.OK.value())
            .body(
                new PactDslJsonBody()
                    .stringValue("message", "Case-User-Role assignments removed successfully")
            )
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "getCaseAssignments")
    public void shouldReturnCaseAssignments(MockServer mockServer) throws Exception {
        JsonPath response = RestAssured
            .given()
            .headers(headers)
            .queryParam("case_ids", "1588234985453946")
            .queryParam("user_ids", "33dff5a7-3b6f-45f1-b5e7-5f9be1ede355")
            .when()
            .get(mockServer.getUrl() + "/case-users")
            .then()
            .statusCode(200)
            .and()
            .extract()
            .body()
            .jsonPath();

        assertThat(response.getString("case_users[0].case_id")).isEqualTo("1588234985453946");
        assertThat(response.getString("case_users[0].user_id")).isEqualTo("33dff5a7-3b6f-45f1-b5e7-5f9be1ede355");
        assertThat(response.getString("case_users[0].case_role")).isEqualTo("[Collaborator]");
    }

    @Test
    @PactTestFor(pactMethod = "getCaseAssignmentsWhenCaseIdsPassed")
    public void shouldReturnBadRequestForGetAssignmentsWhenInputIsMissing(MockServer mockServer) throws Exception {
        JsonPath response = RestAssured
            .given()
            .headers(headers)
            //.queryParam("case_ids", "1588234985453946")
            .when()
            .get(mockServer.getUrl() + "/case-users")
            .then()
            .statusCode(400)
            .and()
            .extract()
            .body()
            .jsonPath();

        assertThat(response.getString("message")).isEqualTo("Case ID list is empty");
    }

    @Test
    @PactTestFor(pactMethod = "addCaseAssignments")
    public void shouldAddCaseAssignments(MockServer mockServer) throws Exception {
        JsonPath response = RestAssured
            .given()
            .headers(headers)
            .body(CASE_ASSIGNMENT_REQUEST)
            .contentType(io.restassured.http.ContentType.JSON)
            .when()
            .post(mockServer.getUrl() + "/case-users")
            .then()
            .statusCode(201)
            .and()
            .extract()
            .body()
            .jsonPath();

        assertThat(response.getString("message")).isEqualTo("Case-User-Role assignments created successfully");
    }

    @Test
    @PactTestFor(pactMethod = "removeCaseAssignments")
    public void shouldRemoveCaseAssignments(MockServer mockServer) throws Exception {
        JsonPath response = RestAssured
            .given()
            .headers(headers)
            .body(CASE_ASSIGNMENT_REQUEST)
            .contentType(io.restassured.http.ContentType.JSON)
            .when()
            .delete(mockServer.getUrl() + "/case-users")
            .then()
            .statusCode(200)
            .and()
            .extract()
            .body()
            .jsonPath();

        assertThat(response.getString("message")).isEqualTo("Case-User-Role assignments removed successfully");
    }

}