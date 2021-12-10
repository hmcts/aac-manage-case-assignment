package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentResponse implements Serializable  {
    private static final long serialVersionUID = -453226127042849422L;

    @JsonProperty("roleAssignmentResponse")
    private List<RoleAssignmentResource> roleAssignments;
}
