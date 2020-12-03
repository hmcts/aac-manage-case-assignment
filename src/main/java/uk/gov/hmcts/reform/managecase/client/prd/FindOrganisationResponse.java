package uk.gov.hmcts.reform.managecase.client.prd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FindOrganisationResponse {

    private List<ContactInformation> contactInformation;
    private String organisationIdentifier;
    private String name;
}
