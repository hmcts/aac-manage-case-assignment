package uk.gov.hmcts.reform.managecase.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class Organisation {

    @JsonProperty("OrganisationID")
    private String organisationID;
    @JsonProperty("OrganisationName")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String organisationName;
}
