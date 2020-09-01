package uk.gov.hmcts.reform.managecase.domain.notify;

public class EmailNotificationRequestFailure extends EmailNotificationRequestStatus {

    private final Exception exception;

    public EmailNotificationRequestFailure(EmailNotificationRequest notificationRequest,
                                           Exception exception) {
        super(notificationRequest);
        this.exception = exception;
    }

    public Exception getException() {
        return this.exception;
    }
}
