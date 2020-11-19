package uk.gov.hmcts.reform.managecase.client.datastore;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseUpdateViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultViewResource;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClientConfig.CASES_WITH_ID;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClientConfig.CASE_USERS;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClientConfig.EXTERNAL_START_EVENT_TRIGGER;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClientConfig.INTERNAL_CASES;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClientConfig.INTERNAL_SEARCH_CASES;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClientConfig.SEARCH_CASES;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClientConfig.START_EVENT_TRIGGER;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClientConfig.SUBMIT_EVENT_FOR_CASE;

@FeignClient(
    name = "data-store-api",
    url = "${ccd.data-store.host}",
    configuration = DataStoreApiClientConfig.class
)
public interface DataStoreApiClient {

    String CASE_ID = "caseId";
    String CASE_TYPE_ID = "ctid";

    @PostMapping(value = SEARCH_CASES, consumes = APPLICATION_JSON_VALUE)
    CaseSearchResponse searchCases(@RequestParam(CASE_TYPE_ID) String caseTypeId,
                                   @RequestBody String jsonSearchRequest);

    @PostMapping(value = INTERNAL_SEARCH_CASES, consumes = APPLICATION_JSON_VALUE)
    CaseSearchResultViewResource internalSearchCases(@RequestParam(CASE_TYPE_ID) String caseTypeId,
                                                     @RequestParam ("use_case") Optional<String> useCase,
                                                     @RequestBody String jsonSearchRequest);

    @PostMapping(value = CASE_USERS, consumes = APPLICATION_JSON_VALUE)
    void assignCase(@RequestBody CaseUserRolesRequest userRolesRequest);

    @GetMapping(CASE_USERS)
    CaseUserRoleResource getCaseAssignments(@RequestParam("case_ids") List<String> caseIds,
                                            @RequestParam("user_ids") List<String> userIds);

    @GetMapping(INTERNAL_CASES)
    CaseViewResource getCaseDetailsByCaseId(@PathVariable(CASE_ID) String caseId);

    @DeleteMapping(value = CASE_USERS, consumes = APPLICATION_JSON_VALUE)
    void removeCaseUserRoles(@RequestBody CaseUserRolesRequest userRolesRequest);

    @GetMapping(START_EVENT_TRIGGER)
    CaseUpdateViewEvent getStartEventTrigger(@PathVariable(CASE_ID) String caseId,
                                                   @PathVariable("eventId") String eventId);

    @GetMapping(EXTERNAL_START_EVENT_TRIGGER)
    StartEventResource getExternalStartEventTrigger(@PathVariable(CASE_ID)String caseId,
                                                    @PathVariable("eventId")String eventId);

    @PostMapping(SUBMIT_EVENT_FOR_CASE)
    CaseDetails submitEventForCase(@PathVariable("caseId") String caseId,
                                    @RequestBody CaseDataContent caseDataContent);

    @GetMapping(CASES_WITH_ID)
    CaseDetails getCaseDetailsByCaseIdViaExternalApi(@PathVariable(CASE_ID) String caseId);


}
