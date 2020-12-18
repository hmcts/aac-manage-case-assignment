package uk.gov.hmcts.reform.managecase.client.prd;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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

    @GetMapping(value = "/refdata/internal/v1/organisations/{orgId}/users?returnRoles=false",
            consumes = APPLICATION_JSON_VALUE)
    FindUsersByOrganisationResponse findActiveUsersByOrganisation(@PathVariable("orgId") String organisationId);
}
