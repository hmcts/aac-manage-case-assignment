package uk.gov.hmcts.reform.managecase.consumer;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue;
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
public class McaExampleConsumerTest {

    private static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private static final String IDAM_OAUTH2_TOKEN = "pact-test-idam-token";
    private static final String SERVICE_AUTHORIZATION_TOKEN = "pact-test-s2s-token";

    static Map<String, String> headers = ImmutableMap.of(
        HttpHeaders.AUTHORIZATION, IDAM_OAUTH2_TOKEN,
        SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN
    );

    private static final String CASE_ASSIGNMENT_REQUEST = "{"
         + "\"case_type_id\": \"DIVORCE\", \"case_id\": \"1588234985453946\", \"assignee_id\": \"abc123\""
        + "}";

    @Pact(provider = "mca", consumer = "mca_example_consumer")
    public RequestResponsePact getCaseAssignments(PactDslWithProvider builder) throws Exception {
        return builder
            .given("MCA successfully return case assignments")
            .uponReceiving("Request to get case assignments")
                .path("/case-assignments")
                .method(HttpMethod.GET.toString())
                .matchQuery("case_ids", "^\\d{16,16}$", "1588234985453946")
                .headers(headers)
            .willRespondWith()
                .status(HttpStatus.OK.value())
                .body(createGetAssignmentsResponse())
            .toPact();
    }

    @Pact(provider = "mca", consumer = "mca_example_consumer")
    public RequestResponsePact validationErrorFromAssignCaseAccess(PactDslWithProvider builder) throws Exception {
        return builder
            .given("MCA throws validation error for assignCaseAccess")
            .uponReceiving("Request to assign case access")
                .path("/case-assignments")
                .method(HttpMethod.POST.toString())
                .body(CASE_ASSIGNMENT_REQUEST, ContentType.APPLICATION_JSON)
                .headers(headers)
            .willRespondWith()
                .status(HttpStatus.BAD_REQUEST.value())
                .body(new PactDslJsonBody()
                        .stringType("message", "Intended assignee has to be in the same organisation")
                        .stringValue("status", "BAD_REQUEST")
                        .eachLike("errors", 1)
                        .closeArray()
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
            .when()
            .get(mockServer.getUrl() + "/case-assignments")
            .then()
            .statusCode(200)
            .and()
            .extract()
            .body()
            .jsonPath();

        assertThat(response.getString("status_message")).isEqualTo("Case-User-Role assignments returned successfully");
        assertThat(response.getString("case_assignments[0].case_id")).isEqualTo("1588234985453946");
        assertThat(response.getString("case_assignments[0].shared_with[0].email")).isEqualTo("John.Smith@gmail.com");
    }

    @Test
    @PactTestFor(pactMethod = "validationErrorFromAssignCaseAccess")
    public void shouldReturn400BadRequestForAssignCaseAccess(MockServer mockServer) throws Exception {
        JsonPath response = RestAssured
            .given()
            .headers(headers)
            .contentType(io.restassured.http.ContentType.JSON)
            .body(CASE_ASSIGNMENT_REQUEST)
            .when()
            .post(mockServer.getUrl() + "/case-assignments")
            .then()
            .statusCode(400)
            .and()
            .extract()
            .body()
            .jsonPath();

        assertThat(response.getString("message")).isEqualTo("Intended assignee has to be in the same organisation");
        assertThat(response.getString("status")).isEqualTo("BAD_REQUEST");
    }

    private PactDslJsonBody createGetAssignmentsResponse() {

        return (PactDslJsonBody) new PactDslJsonBody()
            .stringType("status_message", "Case-User-Role assignments returned successfully")
            .minArrayLike("case_assignments", 1, 1)
                .stringType("case_id", "1588234985453946")
                .minArrayLike("shared_with", 1, 1)
                    .stringType("idam_id", "33dff5a7-3b6f-45f1-b5e7-5f9be1ede355")
                    .stringType("first_name",  "John")
                    .stringType("last_name", "Smith")
                    .stringType("email", "John.Smith@gmail.com")
                    .minArrayLike("case_roles", 1, PactDslJsonRootValue.stringType("[Collaborator]"), 1)
                .closeArray()
            .closeArray();
    }

}
