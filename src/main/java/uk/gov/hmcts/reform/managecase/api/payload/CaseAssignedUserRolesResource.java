package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.RepresentationModel;

import java.util.List;

@Data
@NoArgsConstructor
public class CaseAssignedUserRolesResource extends RepresentationModel<RepresentationModel<?>> {

    @JsonProperty("case_users")
    private List<CaseAssignedUserRole> caseAssignedUserRoles;

    public CaseAssignedUserRolesResource(List<CaseAssignedUserRole> caseAssignedUserRoles) {
        this.caseAssignedUserRoles = caseAssignedUserRoles;
    }

    @Override
    @JsonIgnore
    public Links getLinks() {
        return super.getLinks();
    }
}
