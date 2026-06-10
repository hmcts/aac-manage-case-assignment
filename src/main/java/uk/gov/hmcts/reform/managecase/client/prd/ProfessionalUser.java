package uk.gov.hmcts.reform.managecase.client.prd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ProfessionalUser {

    private String userIdentifier;
    private String firstName;
    private String lastName;
    private String email;
    private String idamStatus;

}
