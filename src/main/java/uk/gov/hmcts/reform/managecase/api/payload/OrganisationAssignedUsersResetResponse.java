package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@ApiModel("Organisation Assigned Users Reset Response")
public class OrganisationAssignedUsersResetResponse {

    @JsonProperty("organisation_user_counts")
    @ApiModelProperty(value = "Organisation user counts", required = true)
    private final Map<String, Long> orgUserCounts;

}
