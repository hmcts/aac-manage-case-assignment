package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseUserRole {

    @JsonProperty("case_id")
    private String caseId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("case_role")
    private String caseRole;
}
