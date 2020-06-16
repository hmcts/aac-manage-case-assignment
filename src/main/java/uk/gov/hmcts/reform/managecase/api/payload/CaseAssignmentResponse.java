package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ApiModel("Case Assignment Response")
public class CaseAssignmentResponse {

    @JsonProperty("status_message")
    @ApiModelProperty(value = "Domain Status Message", required = true, example = "[Defendant]")
    private String status;

}
