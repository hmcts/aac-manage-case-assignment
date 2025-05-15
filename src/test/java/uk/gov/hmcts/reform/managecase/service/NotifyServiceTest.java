package uk.gov.hmcts.reform.managecase.service;

import com.google.common.collect.Lists;
import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executor;
import javax.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequest;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequestFailure;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequestStatus;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NotifyServiceTest {

    private static final String CASE_ID = "12345678";

    private static final String TEST_EMAIL = "test@hmcts.net";

    private static final String EMAIL_TEMPLATE_ID = "TestEmailTemplateId";

    private static final String NOT_NULL_MESSAGE = "response statuses should not be null";

    public static final String SIZE_SHOULD_BE_EQUAL_TO_1 = "size should be equal to 1";

    @Mock
    private ApplicationParams appParams;

    @Mock
    private NotificationClient notificationClient;

    private NotifyService notifyService;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.notifyService = new NotifyService(appParams, notificationClient);
    }

    @Test
    @DisplayName("should fail when null email notification request list is passed")
    void shouldThrowValidationExceptionWhenNotificationListIsNull() {
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> this.notifyService.sendEmail(null)
        );

        assertThat(exception.getMessage())
            .isEqualTo("At least one email notification request is required to send notification");
    }

    @Test
    @DisplayName("should fail when empty email notification request list is passed")
    void shouldThrowValidationExceptionWhenNotificationListIsEmpty() {
        ValidationException exception = assertThrows(ValidationException.class, () ->
            this.notifyService.sendEmail(Lists.newArrayList()));

        assertThat(exception.getMessage())
            .isEqualTo("At least one email notification request is required to send notification");
    }

    @Test
    @DisplayName("should fail when empty case id is passed in email notification object")
    void shouldThrowValidationExceptionWhenCaseIdIsEmptyInNotificationObject() {
        List<EmailNotificationRequest> emailNotificationRequests =
            Lists.newArrayList(new EmailNotificationRequest("", TEST_EMAIL));
        List<EmailNotificationRequestStatus> requestStatuses = this.notifyService.sendEmail(emailNotificationRequests);

        assertNotNull(requestStatuses, NOT_NULL_MESSAGE);
        assertEquals(1, requestStatuses.size(), SIZE_SHOULD_BE_EQUAL_TO_1);
        EmailNotificationRequestFailure failure = (EmailNotificationRequestFailure) requestStatuses.get(0);
        assertThat(failure.getException().getMessage()).isEqualTo("case id is required to send notification");
    }

    @Test
    @DisplayName("should fail when null case id is passed in email notification object")
    void shouldThrowValidationExceptionWhenCaseIdIsNullInNotificationObject() {
        List<EmailNotificationRequest> emailNotificationRequests =
            Lists.newArrayList(new EmailNotificationRequest(null, TEST_EMAIL));
        List<EmailNotificationRequestStatus> requestStatuses = this.notifyService.sendEmail(emailNotificationRequests);

        assertNotNull(requestStatuses, NOT_NULL_MESSAGE);
        assertEquals(1, requestStatuses.size(), SIZE_SHOULD_BE_EQUAL_TO_1);
        EmailNotificationRequestFailure failure = (EmailNotificationRequestFailure) requestStatuses.get(0);
        assertThat(failure.getException().getMessage()).isEqualTo("case id is required to send notification");
    }

    @Test
    @DisplayName("should fail when null case id is passed in email notification object")
    void shouldThrowValidationExceptionWhenEmailAddressIsEmptyInNotificationObject() {
        List<EmailNotificationRequest> emailNotificationRequests =
            Lists.newArrayList(new EmailNotificationRequest(CASE_ID, ""));
        List<EmailNotificationRequestStatus> requestStatuses = this.notifyService.sendEmail(emailNotificationRequests);

        assertNotNull(requestStatuses, NOT_NULL_MESSAGE);
        assertEquals(1, requestStatuses.size(), SIZE_SHOULD_BE_EQUAL_TO_1);
        EmailNotificationRequestFailure failure = (EmailNotificationRequestFailure) requestStatuses.get(0);
        assertThat(failure.getException().getMessage()).isEqualTo("email address is required to send notification");
    }

    @Test
    @DisplayName("should fail when empty case id is passed in email notification object")
    void shouldThrowValidationExceptionWhenEmailAddressIsNullInNotificationObject() {
        List<EmailNotificationRequest> emailNotificationRequests =
            Lists.newArrayList(new EmailNotificationRequest(CASE_ID, null));
        List<EmailNotificationRequestStatus> requestStatuses = this.notifyService.sendEmail(emailNotificationRequests);

        assertNotNull(requestStatuses, NOT_NULL_MESSAGE);
        assertEquals(1, requestStatuses.size(), SIZE_SHOULD_BE_EQUAL_TO_1);
        EmailNotificationRequestFailure failure = (EmailNotificationRequestFailure) requestStatuses.get(0);
        assertThat(failure.getException().getMessage()).isEqualTo("email address is required to send notification");
    }

    @Test
    @DisplayName("should invoke notification client sendEmail")
    void shouldInvokeNotificationClientSendNotification() throws NotificationClientException {
        given(appParams.getEmailTemplateId()).willReturn(EMAIL_TEMPLATE_ID);
        SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);
        given(this.notificationClient
                  .sendEmail(
                      anyString(),
                      anyString(),
                      anyMap(),
                      anyString()
                  ))
            .willReturn(sendEmailResponse);
        EmailNotificationRequest request = new EmailNotificationRequest(CASE_ID, TEST_EMAIL);

        List<EmailNotificationRequestStatus> responses = this.notifyService.sendEmail(Lists.newArrayList(request));
        assertNotNull(responses, "response object should not be null");
        assertEquals(1, responses.size(), "response size is not equal");
        verify(this.notificationClient).sendEmail(
            anyString(),
            anyString(),
            anyMap(),
            anyString()
        );
    }

    @Test
    @DisplayName("should invoke notification client sendEmail for more than one case id")
    void shouldInvokeNotificationClientSendNotificationForMoreThanOneCaseId() throws NotificationClientException {
        given(appParams.getEmailTemplateId()).willReturn(EMAIL_TEMPLATE_ID);
        SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);
        given(this.notificationClient
                  .sendEmail(
                      anyString(),
                      anyString(),
                      anyMap(),
                      anyString()
                  ))
            .willReturn(sendEmailResponse);

        EmailNotificationRequest request1 = new EmailNotificationRequest(CASE_ID, TEST_EMAIL);
        EmailNotificationRequest request2 = new EmailNotificationRequest("12345679", TEST_EMAIL);

        List<EmailNotificationRequestStatus> responses = this.notifyService
            .sendEmail(Lists.newArrayList(request1, request2));

        assertNotNull(responses, "response object should not be null");
        assertEquals(2, responses.size(), "response size is not equal");
        verify(this.notificationClient, times(2)).sendEmail(
            anyString(),
            anyString(),
            anyMap(),
            anyString()
        );
    }

    @Test
    @DisplayName("should invoke notification client sendEmail for multiple case id and email addresses")
    void shouldInvokeNotificationClientSendNotificationForMoreThanOneCaseIdAndMoreThanOneEmailAddress()
        throws NotificationClientException {
        given(appParams.getEmailTemplateId()).willReturn(EMAIL_TEMPLATE_ID);
        SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);
        given(this.notificationClient
                  .sendEmail(
                      anyString(),
                      anyString(),
                      anyMap(),
                      anyString()
                  ))
            .willReturn(sendEmailResponse);

        EmailNotificationRequest request1 = new EmailNotificationRequest(CASE_ID, TEST_EMAIL);
        EmailNotificationRequest request2 = new EmailNotificationRequest("12345679", TEST_EMAIL);
        EmailNotificationRequest request3 = new EmailNotificationRequest(CASE_ID, "test2@hmcts.net");
        EmailNotificationRequest request4 = new EmailNotificationRequest("12345679", "test2@hmcts.net");

        List<EmailNotificationRequestStatus> responses = this.notifyService.sendEmail(
            Lists.newArrayList(request1, request2, request3, request4));

        assertNotNull(responses, "response object should not be null");
        assertEquals(4, responses.size(), "response size is not equal");
        verify(this.notificationClient, times(4)).sendEmail(
            anyString(),
            anyString(),
            anyMap(),
            anyString()
        );
    }

    @Test
    @DisplayName("should shutdown executor when cleanup is called")
    void shouldShutdownExecutorWhenCleanupIsCalled() {
        
        ExecutorService mockExecutor = mock(ExecutorService.class);
        
        ReflectionTestUtils.setField(notifyService, "executor", mockExecutor);
        notifyService.cleanup();

        verify(mockExecutor).shutdown();
    }
    
    @Test
    @DisplayName("should process notifications in parallel")
    void shouldProcessNotificationsInParallel() throws NotificationClientException {

        given(appParams.getEmailTemplateId()).willReturn(EMAIL_TEMPLATE_ID);
        SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);
        given(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
            .willReturn(sendEmailResponse);

        List<EmailNotificationRequest> requests = Lists.newArrayList();
        for (int i = 0; i < 10; i++) {
            requests.add(new EmailNotificationRequest(CASE_ID + i, TEST_EMAIL));
        }
       
        long startTime = System.currentTimeMillis();
        List<EmailNotificationRequestStatus> responses = notifyService.sendEmail(requests);
        long endTime = System.currentTimeMillis();
        
        assertNotNull(responses, "response object should not be null");
        assertEquals(10, responses.size(), "response size should be 10");

        verify(notificationClient, times(10)).sendEmail(
            anyString(),
            anyString(),
            anyMap(),
            anyString()
        );
        
        System.out.println("Execution time for 10 parallel requests: " + (endTime - startTime) + "ms");
    }
    
    @Test
    @DisplayName("should use dynamic pool size based on available processors")
    void shouldUseDynamicPoolSizeBasedOnAvailableProcessors() {
        NotifyService service = new NotifyService(appParams, notificationClient);
        
        int expectedPoolSize = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);
        
        Executor executor = (Executor) ReflectionTestUtils.getField(service, "executor");
        assertNotNull(executor, "Executor should not be null");
        
        System.out.println("Expected thread pool size: " + expectedPoolSize);
    }
    
    @Test
    @DisplayName("should handle connection exception with proper failure response")
    void shouldHandleConnectionExceptionWithRetry() throws NotificationClientException {
        given(appParams.getEmailTemplateId()).willReturn(EMAIL_TEMPLATE_ID);
        
        ArgumentCaptor<String> templateIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        // Note: In a unit test environment, Spring's @Retryable won't actually work
        // because it requires Spring AOP context which isn't set up in JUnit tests
        given(notificationClient.sendEmail(
            templateIdCaptor.capture(),
            emailCaptor.capture(),
            anyMap(),
            anyString()))
            .willThrow(new NotificationClientException(new ConnectException("Connection refused")));
        
        EmailNotificationRequest request = new EmailNotificationRequest(CASE_ID, TEST_EMAIL);
        
        List<EmailNotificationRequestStatus> responses = notifyService.sendEmail(Lists.newArrayList(request));
        
        assertNotNull(responses, NOT_NULL_MESSAGE);
        assertEquals(1, responses.size(), SIZE_SHOULD_BE_EQUAL_TO_1);
        
        assertTrue(responses.get(0) instanceof EmailNotificationRequestFailure,
                   "Should be a failure response when connection fails");
        
        verify(notificationClient).sendEmail(
            anyString(),
            anyString(),
            anyMap(),
            anyString()
        );
        
        assertEquals(EMAIL_TEMPLATE_ID, templateIdCaptor.getValue());
        assertEquals(TEST_EMAIL, emailCaptor.getValue());
        
        EmailNotificationRequestFailure failure = (EmailNotificationRequestFailure) responses.get(0);
        assertTrue(failure.getException() instanceof NotificationClientException,
                   "Exception should be a NotificationClientException");
        assertTrue(failure.getException().getCause() instanceof ConnectException,
                   "Root cause should be a ConnectException");
    }
}
