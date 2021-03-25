package uk.gov.hmcts.reform.managecase.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressUK {

    @JsonProperty("AddressLine1")
    private String addressLine1;
    @JsonProperty("AddressLine2")
    private String addressLine2;
    @JsonProperty("AddressLine3")
    private String addressLine3;
    @JsonProperty("PostTown")
    private String postTown;
    @JsonProperty("County")
    private String county;
    @JsonProperty("Country")
    private String country;
    @JsonProperty("PostCode")
    private String postCode;
}
