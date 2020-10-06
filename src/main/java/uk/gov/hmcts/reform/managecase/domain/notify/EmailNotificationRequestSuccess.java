package uk.gov.hmcts.reform.managecase.domain.notify;

import uk.gov.service.notify.SendEmailResponse;

public class EmailNotificationRequestSuccess extends EmailNotificationRequestStatus {

    private final SendEmailResponse emailResponse;

    public EmailNotificationRequestSuccess(EmailNotificationRequest notificationRequest,
                                           SendEmailResponse emailResponse) {
        super(notificationRequest);
        this.emailResponse = emailResponse;
    }

    public SendEmailResponse getResponse() {
        return this.emailResponse;
    }
}
