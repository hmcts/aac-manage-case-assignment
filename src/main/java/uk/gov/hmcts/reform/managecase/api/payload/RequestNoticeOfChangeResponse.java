package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.managecase.service.noc.ApprovalStatus;

import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE;

@Getter
@Builder
@ApiModel("Request Notice Of Change Response")
public class RequestNoticeOfChangeResponse {

    @JsonProperty("status_message")
    @ApiModelProperty(value = "Domain Status Message", required = true,
        example = REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)
    private final String status;

    @JsonProperty("case_role")
    @ApiModelProperty(value = "Case Role", required = true,
        example = "\"[Claimant]\"")
    private final String caseRole;

    @JsonProperty("approval_status")
    @ApiModelProperty(value = "Approval status", required = true,
        example = "APPROVED")
    private final ApprovalStatus approvalStatus;
}
