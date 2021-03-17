package uk.gov.hmcts.reform.managecase.client.prd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContactInformation {

    private String addressLine1;

    private String addressLine2;

    private String addressLine3;

    private String townCity;

    private String county;

    private String country;

    private String postCode;
}
