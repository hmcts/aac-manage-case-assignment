package uk.gov.hmcts.reform.managecase.client.prd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ContactInformation {

    private String addressLine1;

    private String addressLine2;

    private String addressLine3;

    private String townCity;

    private String county;

    private String country;

    private String postCode;
}
