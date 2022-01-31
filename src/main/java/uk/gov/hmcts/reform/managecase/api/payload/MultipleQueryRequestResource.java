package uk.gov.hmcts.reform.managecase.api.payload;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Getter
@Jacksonized
public class MultipleQueryRequestResource {

    private final List<RoleAssignmentQuery> queryRequests;

}
