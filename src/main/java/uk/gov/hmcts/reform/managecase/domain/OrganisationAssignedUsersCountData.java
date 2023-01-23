package uk.gov.hmcts.reform.managecase.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@ApiModel("Count of each organisation's assigned users for single case")
public class OrganisationAssignedUsersCountData {

    @JsonProperty("case_id")
    @ApiModelProperty(name = "Case ID", example = "1674129395329972")
    private final String caseId;

    @JsonProperty("orgs_assigned_users")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ApiModelProperty(name = "Each organisation's assigned user count", example = "{\n"
        + "   \"QUK822N\": 2,\n"
        + "   \"LESTKK0\": 1\n"
        + "}")
    private final Map<String, Long> orgsAssignedUsers;

    @JsonProperty("skipped_organisations")
    @ApiModelProperty(name = "Organisations skipped", example = "{\n"
        + "   \"BadOrgId\": \"Organisation not found\"\n"
        + "}")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final Map<String, String> skippedOrgs;

    @JsonProperty("error")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final String error;

}
