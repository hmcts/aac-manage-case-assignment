package uk.gov.hmcts.reform.managecase.client.prd;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.reform.managecase.client.ApiClientConfig;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "rd-professional-api",
    url = "${prd.host}",
    configuration = ApiClientConfig.class
)
public interface PrdApiClient {

    @GetMapping(value = "/refdata/external/v1/organisations/users", consumes = APPLICATION_JSON_VALUE)
    FindUsersByOrganisationResponse findUsersByOrganisation(@RequestParam("status") String status);
}
