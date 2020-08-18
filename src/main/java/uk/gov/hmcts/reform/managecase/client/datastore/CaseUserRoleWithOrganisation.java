package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
public class CaseUserRoleWithOrganisation extends CaseUserRole {

    @JsonProperty("organisation_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String organisationId;

    @Builder(builderMethodName = "withOrganisationBuilder")
    public CaseUserRoleWithOrganisation(String caseId, String userId, String caseRole, String organisationId) {
        super(caseId, userId, caseRole);
        this.organisationId = organisationId;
    }

}
