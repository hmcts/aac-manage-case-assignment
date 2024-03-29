package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

import static java.util.Collections.emptyList;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SearchCaseUserRolesRequest {

    @JsonProperty("case_ids")
    private List<String> caseIds;

    @JsonProperty("user_ids")
    private List<String> userIds;

    public List<String> getCaseIds() {
        return caseIds == null ? emptyList() : caseIds;
    }

    public List<String> getUserIds() {
        return userIds == null ? emptyList() : userIds;
    }
}
