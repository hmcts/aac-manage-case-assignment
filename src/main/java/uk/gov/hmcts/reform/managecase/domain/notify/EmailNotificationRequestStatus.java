package uk.gov.hmcts.reform.managecase.domain.notify;

public abstract class EmailNotificationRequestStatus {

    private final EmailNotificationRequest notificationRequest;

    public EmailNotificationRequestStatus(EmailNotificationRequest notificationRequest) {
        this.notificationRequest = notificationRequest;
    }

    public abstract Object getNotificationStatus();

    public EmailNotificationRequest getNotificationRequest() {
        return notificationRequest;
    }

}
