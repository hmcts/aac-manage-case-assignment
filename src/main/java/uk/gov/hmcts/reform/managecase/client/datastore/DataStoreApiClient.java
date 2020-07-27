package uk.gov.hmcts.reform.managecase.client.datastore;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "data-store-api",
    url = "${ccd.data-store.host}",
    configuration = DataStoreApiClientConfig.class
)
public interface DataStoreApiClient {

    @PostMapping(value = "/searchCases", consumes = APPLICATION_JSON_VALUE)
    CaseSearchResponse searchCases(@RequestParam("ctid") String caseTypeId,  @RequestBody String jsonSearchRequest);

    @PostMapping(value = "/case-users", consumes = APPLICATION_JSON_VALUE)
    void assignCase(@RequestBody CaseUserRoleResource userRolesRequest);

    @GetMapping("/case-users")
    CaseUserRoleResource getCaseAssignments(@RequestParam("case_ids") List<String> caseIds,
                                            @RequestParam("user_ids") List<String> userIds);
}
