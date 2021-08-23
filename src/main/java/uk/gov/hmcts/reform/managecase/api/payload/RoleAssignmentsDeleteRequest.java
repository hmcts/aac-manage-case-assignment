package uk.gov.hmcts.reform.managecase.api.payload;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class RoleAssignmentsDeleteRequest {

    private final String caseId;

    private final String userId;

    private final List<String> roleNames;

}
