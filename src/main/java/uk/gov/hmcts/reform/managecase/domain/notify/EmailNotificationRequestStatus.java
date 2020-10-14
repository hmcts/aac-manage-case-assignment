package uk.gov.hmcts.reform.managecase.domain.notify;

import lombok.Getter;

@Getter
public class EmailNotificationRequestStatus {

    private final EmailNotificationRequest notificationRequest;

    protected EmailNotificationRequestStatus(EmailNotificationRequest notificationRequest) {
        this.notificationRequest = notificationRequest;
    }
}
