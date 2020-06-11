package uk.gov.hmcts.reform.managecase.client.prd;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.managecase.client.ApiClientConfig;

@FeignClient(
    name = "rd-professional-api",
    url = "${prd.host}",
    configuration = ApiClientConfig.class
)
public interface PrdApiClient {

    @GetMapping("/refdata/external/v1/organisations/users")
    FindUsersByOrganisationResponse findUsersByOrganisation(@RequestParam("status") String status);
}
