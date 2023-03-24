package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.managecase.domain.OrganisationsAssignedUsersCountData;

import java.util.List;

@Getter
@Builder
@ApiModel("Organisations Assigned Users Reset Response")
public class OrganisationsAssignedUsersResetResponse {

    @JsonProperty("count_data")
    @ApiModelProperty(value = "List of Organisations Assigned Users count data", required = true)
    private final List<OrganisationsAssignedUsersCountData> countData;

}
