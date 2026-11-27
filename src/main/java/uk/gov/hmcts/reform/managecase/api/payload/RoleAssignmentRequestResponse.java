package uk.gov.hmcts.reform.managecase.api.payload;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Value
@Jacksonized
public class RoleAssignmentRequestResponse {

    RoleAssignmentRequestResource roleAssignmentResponse;

}
