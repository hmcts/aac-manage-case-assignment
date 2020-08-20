package uk.gov.hmcts.reform.managecase.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.service.notify.SendEmailResponse;

@Getter
@NoArgsConstructor
public class EmailNotificationResponse {

    @JsonProperty("success_responses")
    private List<SendEmailResponse> successResponses = new ArrayList<>();

    @JsonProperty("failure_responses")
    private List<EmailNotificationFailure> failureResponses = new ArrayList<>();

    public void addSuccessResponse(SendEmailResponse sendEmailResponse) {
        this.successResponses.add(sendEmailResponse);
    }

    public void addFailureResponse(EmailNotificationFailure emailNotificationFailure) {
        this.failureResponses.add(emailNotificationFailure);
    }

    public boolean hasFailures() {
        return failureResponses.size() > 0;
    }
}
