package uk.gov.hmcts.reform.managecase.client.datastore;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseAccessMetadataResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseUpdateViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "data-store-api",
    url = "${ccd.data-store.host}",
    configuration = DataStoreApiClientConfig.class
)
public interface DataStoreApiClient {

    String CASE_ID = "caseId";
    String CASE_TYPE_ID = "ctid";

    String CASES_WITH_ID = "/cases/{caseId}";
    String CASE_USERS = "/case-users";
    String CASE_USERS_SEARCH = CASE_USERS + "/search";
    String INTERNAL_CASES = "/internal/cases/{caseId}";
    String START_EVENT_TRIGGER = INTERNAL_CASES + "/event-triggers/{eventId}";
    String CASES_WITH_ID_ACCESS_METADATA = INTERNAL_CASES + "/access-metadata";
    String SUBMIT_EVENT_FOR_CASE = CASES_WITH_ID + "/events";
    String EXTERNAL_START_EVENT_TRIGGER = "/cases/{caseId}/event-triggers/{eventId}";
    String SUPPLEMENTARY_UPDATE = "/cases/{caseId}/supplementary-data";

    @PostMapping(value = CASE_USERS, consumes = APPLICATION_JSON_VALUE)
    void assignCase(@RequestBody CaseUserRolesRequest userRolesRequest);

    @PostMapping(CASE_USERS_SEARCH)
    CaseUserRoleResource searchCaseAssignments(@RequestBody SearchCaseUserRolesRequest searchRequest);

    @GetMapping(INTERNAL_CASES)
    CaseViewResource getCaseDetailsByCaseId(@RequestHeader(AUTHORIZATION) String userAuthorizationHeader,
                                            @PathVariable(CASE_ID) String caseId);

    @DeleteMapping(value = CASE_USERS, consumes = APPLICATION_JSON_VALUE)
    void removeCaseUserRoles(@RequestBody CaseUserRolesRequest userRolesRequest);

    @GetMapping(CASES_WITH_ID_ACCESS_METADATA)
    CaseAccessMetadataResource getCaseAccessMetadataByCaseId(
        @RequestHeader(AUTHORIZATION) String userAuthorizationHeader,
        @PathVariable(CASE_ID) String caseId);

    @GetMapping(START_EVENT_TRIGGER)
    CaseUpdateViewEvent getStartEventTrigger(@RequestHeader(AUTHORIZATION) String userAuthorizationHeader,
                                             @PathVariable(CASE_ID) String caseId,
                                             @PathVariable("eventId") String eventId);

    @GetMapping(EXTERNAL_START_EVENT_TRIGGER)
    StartEventResource getExternalStartEventTrigger(@RequestHeader(AUTHORIZATION) String userAuthorizationHeader,
                                                    @PathVariable(CASE_ID) String caseId,
                                                    @PathVariable("eventId") String eventId);

    @PostMapping(SUBMIT_EVENT_FOR_CASE)
    CaseDetails submitEventForCase(@RequestHeader(AUTHORIZATION) String userAuthorizationHeader,
                                    @PathVariable(CASE_ID) String caseId,
                                    @RequestBody CaseEventCreationPayload caseEventCreationPayload);

    @GetMapping(CASES_WITH_ID)
    CaseDetails getCaseDetailsByCaseIdViaExternalApi(@RequestHeader(AUTHORIZATION) String userAuthorizationHeader,
                                                     @PathVariable(CASE_ID) String caseId);

    @PostMapping(SUPPLEMENTARY_UPDATE)
    void updateCaseSupplementaryData(@RequestHeader(AUTHORIZATION) String userAuthorizationHeader,
                                   @PathVariable(CASE_ID) String caseId,
                                   SupplementaryDataUpdateRequest supplementaryDataUpdateRequest);

}
