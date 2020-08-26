package uk.gov.hmcts.reform.managecase.domain.notify;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class EmailNotificationRequest {

    @JsonProperty("case_id")
    private String caseId;

    @JsonProperty("email_address")
    private String emailAddress;
}
