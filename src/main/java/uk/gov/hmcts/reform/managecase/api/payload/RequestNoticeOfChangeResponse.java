package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE;

@Getter
@Builder
@AllArgsConstructor
@ApiModel("Request Notice Of Change Response")
public class RequestNoticeOfChangeResponse {

    @JsonProperty("status_message")
    @ApiModelProperty(value = "Domain Status Message", required = true,
        example = REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)
    private String status;

    @JsonProperty("case_role")
    private String caseRole;

    @JsonProperty("approval_status")
    private String approvalStatus;
}
