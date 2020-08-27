package uk.gov.hmcts.reform.managecase.domain.notify;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class EmailNotificationRequestStatus {

    private final EmailNotificationRequest notificationRequest;

    public abstract Object getNotificationStatus();

}
