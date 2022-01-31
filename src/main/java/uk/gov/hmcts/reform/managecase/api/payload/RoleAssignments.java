package uk.gov.hmcts.reform.managecase.api.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleAssignments {
    private List<RoleAssignment> roleAssignmentsList;

}
