package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CaseAssignedUserRolesRequest {

    @JsonProperty("case_users")
    private List<CaseAssignedUserRoleWithOrganisation> caseAssignedUserRoles;

}
