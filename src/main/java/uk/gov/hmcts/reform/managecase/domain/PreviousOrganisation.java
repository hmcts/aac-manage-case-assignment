package uk.gov.hmcts.reform.managecase.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PreviousOrganisation {

    @JsonProperty("OrganisationName")
    private String organisationName;
    @JsonProperty("FromTimestamp")
    private LocalDateTime fromTimestamp;
    @JsonProperty("ToTimestamp")
    private LocalDateTime toTimestamp;
    @JsonProperty("OrganisationAddress")
    private AddressUK organisationAddress;
}
