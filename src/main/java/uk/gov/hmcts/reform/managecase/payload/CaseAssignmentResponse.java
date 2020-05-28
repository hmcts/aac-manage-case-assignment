package uk.gov.hmcts.reform.managecase.payload;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CaseAssignmentResponse {

    @JsonProperty("status_message")
    private String status;

}
