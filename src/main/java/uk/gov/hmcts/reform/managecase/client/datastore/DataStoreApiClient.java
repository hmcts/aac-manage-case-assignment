package uk.gov.hmcts.reform.managecase.client.datastore;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@FeignClient(name = "data-store-api", url = "${ccd.data-store.host}", configuration = DataStoreApiClientConfig.class)
public interface DataStoreApiClient {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @RequestMapping(method = RequestMethod.GET, value = "/cases/{caseId}")
    // FIXME : change to /searchCases once RDM-8587 is completed
    CaseDetails findCaseById(@PathVariable("caseId") String caseId);
}

