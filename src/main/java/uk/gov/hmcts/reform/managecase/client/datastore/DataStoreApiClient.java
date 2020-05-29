package uk.gov.hmcts.reform.managecase.client.datastore;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(
    name = "data-store-api",
    url = "${ccd.data-store.host}",
    configuration = DataStoreApiClientConfig.class
)
public interface DataStoreApiClient {

    @PostMapping("/searchCases")
    List<CaseDetails> searchCases(@RequestBody String jsonSearchRequest);
}

