package uk.gov.hmcts.reform.managecase.fixtures;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.managecase.TestFixtures;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseEventCreationPayload;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseSearchResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRolesRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.StartEventResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseUpdateViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultViewResource;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.util.Lists.list;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient.CASE_USERS;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient.INTERNAL_SEARCH_CASES;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient.SEARCH_CASES;

@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports"})
public class WiremockFixtures {

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";
    public static final String SYS_USER_TOKEN = "Bearer eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODM0NDUyOTd9aa";
    public static final String APPROVER_USER_TOKEN = "Bearer eyJzdWIiOiJjY6ShZ3ciLCJleHAiOlD1OEM0NDUyOTd7co";
    public static final String S2S_TOKEN = "Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODM0NDUyOTd9"
        + ".WWRzROlKxLQCJw5h0h0dHb9hHfbBhF2Idwv1z4L4FnqSw3VZ38ZRLuDmwr3tj-8oOv6EfLAxV0dJAPtUT203Iw";

    private static final ObjectMapper OBJECT_MAPPER = new Jackson2ObjectMapperBuilder()
        .modules(new Jdk8Module())
        .build();

    private WiremockFixtures() {
    }

    // Same issue as here https://github.com/tomakehurst/wiremock/issues/97
    public static class ConnectionClosedTransformer extends ResponseDefinitionTransformer {

        @Override
        public String getName() {
            return "keep-alive-disabler";
        }

        @Override
        public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition,
                                            FileSource files, Parameters parameters) {
            return ResponseDefinitionBuilder.like(responseDefinition)
                .withHeader(HttpHeaders.CONNECTION, "close")
                .build();
        }
    }

    public static void stubGetUsersByOrganisationExternal(FindUsersByOrganisationResponse response) {
        stubFor(WireMock.get(urlEqualTo("/refdata/external/v1/organisations/users?status=Active&returnRoles=false"))
                    .willReturn(okForJson(response)));
    }

    public static void stubGetUsersByOrganisationInternal(FindUsersByOrganisationResponse response, String orgId) {
        stubFor(WireMock.get(urlEqualTo(String.format("/refdata/internal/v1/organisations/%s/users?returnRoles=false",
            orgId))).willReturn(okForJson(response)));
    }

    public static void stubSearchCaseWithPrefix(String caseTypeId, String searchQuery,
                                                CaseDetails caseDetails, String prefix) {
        stubFor(WireMock.post(urlEqualTo(prefix + SEARCH_CASES + "?ctid=" + caseTypeId))
                    .withRequestBody(equalToJson(searchQuery))
                    .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(getJsonString(
                                        caseDetails == null ? new CaseSearchResponse() : new CaseSearchResponse(list(
                                            caseDetails))))
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubSearchCase(String caseTypeId, String searchQuery, CaseDetails caseDetails) {
        stubSearchCaseWithPrefix(caseTypeId, searchQuery, caseDetails, "");
    }

    public static void stubAssignCase(String caseId, String userId, String... caseRoles) {
        stubFor(WireMock.post(urlEqualTo(CASE_USERS))
                    .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                    .withRequestBody(matchingJsonPath("$.case_users[0].case_id", equalTo(caseId)))
                    .withRequestBody(matchingJsonPath("$.case_users[0].case_role", equalTo(caseRoles[0])))
                    .withRequestBody(matchingJsonPath("$.case_users[0].user_id", equalTo(userId)))
                    .withRequestBody(matchingJsonPath(
                        "$.case_users[0].organisation_id",
                        equalTo(TestFixtures.ORGANIZATION_ID)
                    ))
                    .willReturn(aResponse().withStatus(HTTP_OK)));
    }

    public static void stubUnassignCase(List<CaseUserRoleWithOrganisation> unassignments)
        throws JsonProcessingException {

        stubFor(WireMock.delete(urlEqualTo(CASE_USERS))
                    .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                    .withRequestBody(
                        equalToJson(
                            OBJECT_MAPPER.writeValueAsString(new CaseUserRolesRequest(unassignments)),
                            true,
                            false
                        ))
                    .willReturn(aResponse().withStatus(HTTP_OK)));
    }

    public static void stubGetCaseAssignments(List<String> caseIds, List<String> userIds,
                                              List<CaseUserRole> caseUserRoles) {
        stubFor(WireMock.get(urlPathEqualTo(CASE_USERS))
                    .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                    .withQueryParam("case_ids", equalTo(caseIds.get(0)))
                    .withQueryParam("user_ids", equalTo(userIds.get(0)))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(getJsonString(new CaseUserRoleResource(caseUserRoles)))
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubGetCaseAssignments(List<String> caseIds, List<CaseUserRole> caseUserRoles) {
        stubFor(WireMock.get(urlPathEqualTo(CASE_USERS))
            .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
            .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
            .withQueryParam("case_ids", equalTo(caseIds.get(0)))
            .willReturn(aResponse()
                .withStatus(HTTP_OK)
                .withBody(getJsonString(new CaseUserRoleResource(caseUserRoles)))
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubGetCaseInternalES(String caseTypeId,
                                             String searchQuery,
                                             CaseSearchResultViewResource resource) {
        stubFor(WireMock.post(urlEqualTo(INTERNAL_SEARCH_CASES + "?ctid=" + caseTypeId))
                    .withRequestBody(equalToJson(searchQuery))
                    .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK).withBody(getJsonString(resource))
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubGetCaseInternal(String caseId, CaseViewResource caseViewResource) {

        stubFor(WireMock.get(urlPathEqualTo("/internal/cases/" + caseId))
                    .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK).withBody(getJsonString(caseViewResource))
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubGetChallengeQuestions(String caseTypeId,
                                                 String id,
                                                 ChallengeQuestionsResult challengeQuestionsResult) {
        stubFor(WireMock.get(urlPathEqualTo("/api/display/challenge-questions/case-type/"
                                                + caseTypeId
                                                + "/question-groups/"
                                                + id))
                    .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK).withBody(getJsonString(challengeQuestionsResult))
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubGetCaseRoles(String userId, String jurisdiction, String caseTypeId,
                                        List<CaseRole> caseRoles) {

        stubFor(WireMock.get(urlPathEqualTo("/api/data/caseworkers/" + userId + "/jurisdictions/"
                                                + jurisdiction + "/case-types/" + caseTypeId + "/roles"))
                    .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK).withBody(getJsonString(caseRoles))
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubIdamGetUserById(String userId, UserDetails user) {
        stubFor(WireMock.get(urlPathEqualTo("/api/v1/users/" + userId))
                .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                .willReturn(aResponse()
                        .withStatus(HTTP_OK).withBody(getJsonString(user))
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubGetStartEventTrigger(String caseId,
                                                String eventId,
                                                CaseUpdateViewEvent caseUpdateViewEvent) {
        stubFor(WireMock.get(urlPathEqualTo("/internal/cases/" + caseId + "/event-triggers/" + eventId))
                    .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(getJsonString(caseUpdateViewEvent))
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubGetCaseViaExternalApi(String caseId, CaseDetails caseDetails) {
        stubFor(WireMock.get(urlPathEqualTo("/cases/" + caseId))
                    .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK)
                                    .withBody(getJsonString(caseDetails))
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    // APPROVER INTERCEPTORS BELOW
    public static void stubGetCaseInternalAsApprover(String caseId, CaseViewResource caseViewResource) {

        stubFor(WireMock.get(urlPathEqualTo("/internal/cases/" + caseId))
                    .withHeader(AUTHORIZATION, equalTo(APPROVER_USER_TOKEN))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK).withBody(getJsonString(caseViewResource))
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubGetExternalStartEventTrigger(String caseId,
                                                                  String eventId,
                                                                  StartEventResource startEventResource) {
        stubFor(WireMock.get(urlPathEqualTo("/cases/" + caseId + "/event-triggers/" + eventId))
            .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
            .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
            .willReturn(aResponse()
                .withStatus(HTTP_OK)
                .withBody(getJsonString(startEventResource))
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubGetExternalStartEventTriggerAsApprover(String caseId,
                                                          String eventId,
                                                          StartEventResource startEventResource) {
        stubFor(WireMock.get(urlPathEqualTo("/cases/" + caseId + "/event-triggers/" + eventId))
            .withHeader(AUTHORIZATION, equalTo(APPROVER_USER_TOKEN))
            .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
            .willReturn(aResponse()
                .withStatus(HTTP_OK)
                .withBody(getJsonString(startEventResource))
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubSubmitEventForCase(String caseId,
                                              CaseEventCreationPayload caseEventCreationPayload,
                                              CaseDetails caseDetails) throws JsonProcessingException {
        stubFor(WireMock.post(urlPathEqualTo("/cases/" + caseId + "/events"))
                    .withHeader(AUTHORIZATION, equalTo(APPROVER_USER_TOKEN))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                    .withRequestBody(equalToJson(
                        OBJECT_MAPPER.writeValueAsString(caseEventCreationPayload)
                    ))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_CREATED)
                                    .withBody(getJsonString(caseDetails))
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubSubmitEventForCase(String caseID,
                                              CaseDetails caseDetails) {
        stubFor(WireMock.post(urlEqualTo("/cases/" + caseID + "/events"))
                    .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                    .willReturn(aResponse()
                                    .withStatus(HTTP_OK).withBody(getJsonString(caseDetails))
                                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)));
    }

    public static void stubGetCaseDetailsByCaseIdViaExternalApi(String caseId, CaseDetails caseDetails) {
        stubFor(WireMock.get(urlEqualTo("/cases/" + caseId))
                    .withHeader(AUTHORIZATION, equalTo(SYS_USER_TOKEN))
                    .withHeader(SERVICE_AUTHORIZATION, equalTo(S2S_TOKEN))
                    .willReturn(aResponse()
                                    .withStatus(caseDetails == null ? HTTP_NOT_FOUND : HTTP_OK)
                                    .withBody(getJsonString(caseDetails))
                                    .withHeader("Content-Type", "application/json")));
    }

    @SuppressWarnings({"PMD.AvoidThrowingRawExceptionTypes", "squid:S112"})
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
