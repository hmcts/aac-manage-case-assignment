package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class CaseAssignedUserRolesResource {

    @JsonProperty("case_users")
    private List<CaseAssignedUserRole> caseAssignedUserRoles;

    public CaseAssignedUserRolesResource(List<CaseAssignedUserRole> caseAssignedUserRoles) {
        this.caseAssignedUserRoles = caseAssignedUserRoles;
    }
}
