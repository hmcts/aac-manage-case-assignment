package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.managecase.domain.ApprovalStatus;

import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE;

@Getter
@Builder
@Schema(description = "Request Notice Of Change Response")
public class RequestNoticeOfChangeResponse {

    @JsonProperty("status_message")
    @Schema(description = "Domain Status Message", required = true,
        example = REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)
    private final String status;

    @JsonProperty("case_role")
    @Schema(description = "Case Role", required = true,
        example = "\"[Claimant]\"")
    private final String caseRole;

    @JsonProperty("approval_status")
    @Schema(description = "Approval status", required = true,
        example = "APPROVED")
    private final ApprovalStatus approvalStatus;
}
