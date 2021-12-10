package uk.gov.hmcts.reform.managecase.api.payload;

import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;

import java.util.List;

@Builder
@Getter
public class RoleAssignmentsAddRequest {

    private final CaseDetails caseDetails;

    private final String userId;

    private final List<String> roleNames;

}
