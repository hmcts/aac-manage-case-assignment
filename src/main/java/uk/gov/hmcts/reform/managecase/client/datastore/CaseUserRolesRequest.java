package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

import static com.fasterxml.jackson.annotation.Nulls.AS_EMPTY;

@Getter
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseUserRolesRequest {

    @JsonProperty("case_users")
    @JsonSetter(nulls = AS_EMPTY)
    private final List<CaseUserRoleWithOrganisation> caseUsers;
}
