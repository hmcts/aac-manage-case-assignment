package uk.gov.hmcts.reform.managecase.api.controller;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.managecase.BaseTest;
import uk.gov.hmcts.reform.managecase.domain.EmailNotificationResponse;
import uk.gov.hmcts.reform.managecase.service.NotifyService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotifyServiceIT extends BaseTest {

    @Autowired
    private NotifyService notifyService;

    @Test
    public void testEmailNotification() {

        EmailNotificationResponse emailNotificationResponse =
            this.notifyService.senEmail(
                Lists.newArrayList("22331112222"),
                Lists.newArrayList("kiran.yenigala@hmcts.net"));

        assertNotNull(emailNotificationResponse);
        assertEquals(1, emailNotificationResponse.getSuccessResponses().size());
    }

}
