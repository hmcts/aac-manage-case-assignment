package uk.gov.hmcts.reform.managecase.api.payload;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Value
@Jacksonized
public class RoleAssignmentRequestResource {

    RoleRequestResource roleRequest;

    List<RoleAssignmentResource> requestedRoles;

}
