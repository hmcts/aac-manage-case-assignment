package uk.gov.hmcts.reform.managecase.fixtures;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseSearchResponse;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.util.Lists.list;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public final class WiremockFixtures {

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String SYS_USER_TOKEN = "Bearer eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODM0NDUyOTd9aa";
    public static final String S2S_TOKEN = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODM0NDUyOTd9"
            + ".WWRzROlKxLQCJw5h0h0dHb9hHfbBhF2Idwv1z4L4FnqSw3VZ38ZRLuDmwr3tj-8oOv6EfLAxV0dJAPtUT203Iw";

    private static final ObjectMapper OBJECT_MAPPER = new Jackson2ObjectMapperBuilder()
        .modules(new Jdk8Module())
        .build();

    private WiremockFixtures() {
    }

    public static void stubS2SDetails(String serviceName) {
        stubFor(WireMock.get(urlEqualTo("/s2s/details")).willReturn(okJson(serviceName)));
    }

    public static void stubGetUsersByOrganisation(FindUsersByOrganisationResponse response) {
        stubFor(WireMock.get(urlEqualTo("/refdata/external/v1/organisations/users?status=Active"))
                .willReturn(okForJson(response)));
    }

    public static void stubSearchCaseWithPrefix(String caseTypeId, String searchQuery,
                                                CaseDetails caseDetails, String prefix) {
        stubFor(WireMock.post(urlEqualTo(prefix + "/searchCases?ctid=" + caseTypeId))
                .withRequestBody(equalToJson(searchQuery))
                .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK).withBody(getJsonString(new CaseSearchResponse(list(caseDetails))))
                        .withHeader("Content-Type", "application/json")));
    }

    public static void stubSearchCase(String caseTypeId, String searchQuery, CaseDetails caseDetails) {
        stubSearchCaseWithPrefix(caseTypeId, searchQuery, caseDetails, "");
    }

    public static void stubAssignCase(String caseId, String userId, String... caseRoles) {
        stubFor(WireMock.post(urlEqualTo("/case-users"))
                .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                .withRequestBody(matchingJsonPath("$.case_users[0].case_id", equalTo(caseId)))
                .withRequestBody(matchingJsonPath("$.case_users[0].case_role", equalTo(caseRoles[0])))
                .withRequestBody(matchingJsonPath("$.case_users[0].user_id", equalTo(userId)))
                .willReturn(aResponse().withStatus(HTTP_OK)));
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    // Required as wiremock's Json.getObjectMapper().registerModule(..); not working
    // see https://github.com/tomakehurst/wiremock/issues/1127
    private static String getJsonString(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
