package uk.gov.hmcts.reform.managecase.api.controller;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.managecase.BaseIT;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequest;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequestStatus;
import uk.gov.hmcts.reform.managecase.service.NotifyService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotifyServiceIT extends BaseIT {

    @Autowired
    private NotifyService notifyService;

    @Test
    public void testEmailNotification() {

        EmailNotificationRequest request =
            new EmailNotificationRequest("22331112222", "simulate-delivered@notifications.service.gov.uk");
        List<EmailNotificationRequestStatus> notificationRequestStatuses =
            this.notifyService.sendEmail(Lists.newArrayList(request));

        assertNotNull(notificationRequestStatuses, "response object should not be null");
        assertEquals(1, notificationRequestStatuses.size(), "size should equals 1");
    }

}
