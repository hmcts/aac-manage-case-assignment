package uk.gov.hmcts.reform.managecase.client.datastore;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.managecase.client.ApiClientConfig;

@FeignClient(
    name = "data-store-api",
    url = "${ccd.data-store.host}",
    configuration = ApiClientConfig.class
)
public interface DataStoreApiClient {

    @PostMapping("/searchCases")
    CaseSearchResponse searchCases(@RequestParam("ctid") String caseTypeId,  @RequestBody String jsonSearchRequest);

    @PostMapping("/case-users")
    void assignCase(@RequestBody CaseUserRole request);
}
