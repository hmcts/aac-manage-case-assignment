package uk.gov.hmcts.reform.managecase.client.prd;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "rd-professional-api",
    url = "${prd.host}",
    configuration = PrdApiClientConfig.class
)
public interface PrdApiClient {

    @GetMapping(value = "/refdata/external/v1/organisations/users?status=Active&returnRoles=false",
            consumes = APPLICATION_JSON_VALUE)
    FindUsersByOrganisationResponse findActiveUsersByOrganisation();
}
