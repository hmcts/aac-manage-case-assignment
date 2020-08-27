package uk.gov.hmcts.reform.managecase.domain.notify;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailNotificationRequest {

    @JsonProperty("case_id")
    private String caseId;

    @JsonProperty("email_address")
    private String emailAddress;
}
