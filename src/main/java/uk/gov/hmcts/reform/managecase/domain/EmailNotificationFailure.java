package uk.gov.hmcts.reform.managecase.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EmailNotificationFailure {

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("email_address")
    private String emailAddress;

    @JsonProperty("case_id")
    private String caseId;

}
