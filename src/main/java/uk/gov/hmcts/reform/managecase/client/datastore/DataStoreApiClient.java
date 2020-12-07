package uk.gov.hmcts.reform.managecase.client.datastore;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClientConfig.CASES_WITH_ID;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClientConfig.CASE_USERS;
import static uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClientConfig.SEARCH_CASES;

@FeignClient(
    name = "data-store-api",
    url = "${ccd.data-store.host}",
    configuration = DataStoreApiClientConfig.class
)
public interface DataStoreApiClient {

    @PostMapping(value = SEARCH_CASES, consumes = APPLICATION_JSON_VALUE)
    CaseSearchResponse searchCases(@RequestParam("ctid") String caseTypeId,  @RequestBody String jsonSearchRequest);

    @PostMapping(value = CASE_USERS, consumes = APPLICATION_JSON_VALUE)
    void assignCase(@RequestBody CaseUserRolesRequest userRolesRequest);

    @GetMapping(CASE_USERS)
    CaseUserRoleResource getCaseAssignments(@RequestParam("case_ids") List<String> caseIds,
                                            @RequestParam("user_ids") List<String> userIds);

    @DeleteMapping(value = CASE_USERS, consumes = APPLICATION_JSON_VALUE)
    void removeCaseUserRoles(@RequestBody CaseUserRolesRequest userRolesRequest);

    @GetMapping(CASES_WITH_ID)
    CaseDetails getCaseDetailsByCaseIdViaExternalApi(@PathVariable("caseId") String caseId);

}
