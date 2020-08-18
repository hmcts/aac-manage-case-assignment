package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
public class CaseUserRole {

    @JsonProperty("case_id")
    private final String caseId;

    @JsonProperty("user_id")
    private final String userId;

    @JsonProperty("case_role")
    private final String caseRole;

}
