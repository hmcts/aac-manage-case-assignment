package uk.gov.hmcts.reform.managecase.client.datastore;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseUpdateViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;

import java.util.List;

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
    String INTERNAL_CASES = "/internal/cases/{caseId}";
    String START_EVENT_TRIGGER = INTERNAL_CASES + "/event-triggers/{eventId}";
    String SUBMIT_EVENT_FOR_CASE = CASES_WITH_ID + "/events";
    String EXTERNAL_START_EVENT_TRIGGER = "/cases/{caseId}/event-triggers/{eventId}";
    String SUPPLEMENTARY_UPDATE = "/cases/{caseId}/supplementary-data";

    @PostMapping(value = CASE_USERS, consumes = APPLICATION_JSON_VALUE)
    void assignCase(@RequestBody CaseUserRolesRequest userRolesRequest);

    @GetMapping(CASE_USERS)
    CaseUserRoleResource getCaseAssignments(@RequestParam("case_ids") List<String> caseIds,
                                            @RequestParam("user_ids") List<String> userIds);

    @GetMapping(INTERNAL_CASES)
    CaseViewResource getCaseDetailsByCaseId(@RequestHeader(AUTHORIZATION) String userAuthorizationHeader,
                                            @PathVariable(CASE_ID) String caseId);

    @DeleteMapping(value = CASE_USERS, consumes = APPLICATION_JSON_VALUE)
    void removeCaseUserRoles(@RequestBody CaseUserRolesRequest userRolesRequest);

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
    void updateCaseSupplementaryData(
                                   @PathVariable(CASE_ID) String caseId,
                                   SupplementaryDataUpdateRequest supplementaryDataUpdateRequest);

}
