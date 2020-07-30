package uk.gov.hmcts.reform.managecase.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CaseAssignedUsers {

    @JsonProperty("case_id")
    private String caseId;

    @JsonProperty("shared_with")
    private List<UserDetails> users;

}
