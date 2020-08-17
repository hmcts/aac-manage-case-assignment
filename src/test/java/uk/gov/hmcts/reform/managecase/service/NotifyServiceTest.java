package uk.gov.hmcts.reform.managecase.service;

import com.google.common.collect.Lists;
import java.util.List;
import javax.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotifyServiceTest {

    @Mock
    private ApplicationParams appParams;

    @Mock
    private NotificationClient notificationClient;

    private NotifyService notifyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        this.notifyService = new NotifyService(appParams, notificationClient);
    }

    @Test
    @DisplayName("should fail when case id list is null")
    void shouldThrowValidationExceptionWhenCaseIdsIsNull() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            this.notifyService.senEmail(null,
                                        Lists.newArrayList("test@hmcts.net")));

        assertThat(exception.getMessage(), is("At least one case id is required to send notification"));
    }

    @Test
    @DisplayName("should fail when case id list is empty")
    void shouldThrowValidationExceptionWhenCaseIdsIsEmpty() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            this.notifyService.senEmail(Lists.newArrayList(),
                                        Lists.newArrayList("test@hmcts.net")));

        assertThat(exception.getMessage(), is("At least one case id is required to send notification"));
    }


    @Test
    @DisplayName("should fail when email addresses list is null")
    void shouldThrowValidationExceptionWhenEmailAddressesIsNull() {
        String emailTemplateId = "TestEmailTemplateId";
        String replyToEmailId = "noreply@hmcts.net";
        given(appParams.getReplyToEmailId()).willReturn(replyToEmailId);
        given(appParams.getEmailTemplateId()).willReturn(emailTemplateId);

        ValidationException exception = assertThrows(ValidationException.class, () ->
            this.notifyService.senEmail(Lists.newArrayList("12345678"),
                                        null));

        assertThat(exception.getMessage(), is("At least one email address is required to send notification"));
    }

    @Test
    @DisplayName("should fail when email addresses list is empty")
    void shouldThrowValidationExceptionWhenEmailAddressesIsEmpty() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            this.notifyService.senEmail(Lists.newArrayList("12345678"),
                                        Lists.newArrayList()));

        assertThat(exception.getMessage(), is("At least one email address is required to send notification"));
    }

    @Test
    @DisplayName("should invoke notification client sendEmail")
    void shouldInvokeNotificationClientSendNotification() throws NotificationClientException {
        String emailTemplateId = "TestEmailTemplateId";
        String replyToEmailId = "noreply@hmcts.net";
        given(appParams.getReplyToEmailId()).willReturn(replyToEmailId);
        given(appParams.getEmailTemplateId()).willReturn(emailTemplateId);
        SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);
        given(this.notificationClient
                  .sendEmail(
                      anyString(),
                      anyString(),
                      anyMap(),
                      anyString(),
                      anyString()
                  ))
            .willReturn(sendEmailResponse);

        List<SendEmailResponse> responses = this.notifyService.senEmail(Lists.newArrayList("12345678"),
                                                                        Lists.newArrayList("test@hmcts.net"));
        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(this.notificationClient).sendEmail(
            anyString(),
            anyString(),
            anyMap(),
            anyString(),
            anyString()
        );
    }

    @Test
    @DisplayName("should invoke notification client sendEmail for more than one case id")
    void shouldInvokeNotificationClientSendNotificationForMoreThanOneCaseId() throws NotificationClientException {
        String emailTemplateId = "TestEmailTemplateId";
        String replyToEmailId = "noreply@hmcts.net";
        given(appParams.getReplyToEmailId()).willReturn(replyToEmailId);
        given(appParams.getEmailTemplateId()).willReturn(emailTemplateId);
        SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);
        given(this.notificationClient
                  .sendEmail(
                      anyString(),
                      anyString(),
                      anyMap(),
                      anyString(),
                      anyString()
                  ))
            .willReturn(sendEmailResponse);

        List<SendEmailResponse> responses = this.notifyService.senEmail(Lists.newArrayList("12345678", "12345679"),
                                                                        Lists.newArrayList("test@hmcts.net"));
        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(this.notificationClient, times(2)).sendEmail(
            anyString(),
            anyString(),
            anyMap(),
            anyString(),
            anyString()
        );
    }

    @Test
    @DisplayName("should invoke notification client sendEmail for multiple case id and email addresses")
    void shouldInvokeNotificationClientSendNotificationForMoreThanOneCaseIdAndMoreThanOneEmailAddress()
        throws NotificationClientException {
        String emailTemplateId = "TestEmailTemplateId";
        String replyToEmailId = "noreply@hmcts.net";
        given(appParams.getReplyToEmailId()).willReturn(replyToEmailId);
        given(appParams.getEmailTemplateId()).willReturn(emailTemplateId);
        SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);
        given(this.notificationClient
                  .sendEmail(
                      anyString(),
                      anyString(),
                      anyMap(),
                      anyString(),
                      anyString()
                  ))
            .willReturn(sendEmailResponse);

        List<SendEmailResponse> responses = this.notifyService.senEmail(
            Lists.newArrayList("12345678", "12345679"),
            Lists.newArrayList("test@hmcts.net", "test2@hmcts.net")
        );
        assertNotNull(responses);
        assertEquals(4, responses.size());
        verify(this.notificationClient, times(4)).sendEmail(
            anyString(),
            anyString(),
            anyMap(),
            anyString(),
            anyString()
        );
    }
}
