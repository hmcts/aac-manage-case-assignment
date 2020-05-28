package uk.gov.hmcts.reform.managecase.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.validation.constraints.NotEmpty;

@Getter
@AllArgsConstructor
public class CaseAssignmentRequest {

    @JsonProperty("case_id")
    @NotEmpty(message = "Case ID can not be empty")
    private String caseId;

    @JsonProperty("assignee_id")
    @NotEmpty(message = "IDAM Assignee ID can not be empty")
    private String assigneeId;
}
