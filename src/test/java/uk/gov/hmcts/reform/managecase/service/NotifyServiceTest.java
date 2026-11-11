package uk.gov.hmcts.reform.managecase.service;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequest;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequestFailure;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequestStatus;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequestSuccess;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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
        this.notifyService = new NotifyService(appParams, notificationClient, Runnable::run);
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
    void shouldSendNotificationsConcurrentlyAndWaitForCompletion() throws Exception {
        given(appParams.getEmailTemplateId()).willReturn(EMAIL_TEMPLATE_ID);
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);
        given(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString())).willAnswer(invocation -> {
            started.countDown();
            if (!release.await(5, SECONDS)) {
                throw new IllegalStateException("Notification was not released by the test");
            }
            return sendEmailResponse;
        });
        List<EmailNotificationRequest> requests = List.of(
            new EmailNotificationRequest(CASE_ID, TEST_EMAIL),
            new EmailNotificationRequest("12345679", "second@hmcts.net"));

        try (ExecutorService workers = Executors.newFixedThreadPool(2);
             ExecutorService caller = Executors.newSingleThreadExecutor()) {
            NotifyService concurrentService = new NotifyService(appParams, notificationClient, workers);
            Future<List<EmailNotificationRequestStatus>> result = caller.submit(
                () -> concurrentService.sendEmail(requests));
            try {
                assertTrue(started.await(5, SECONDS), "Both notifications must start before either completes");
                assertFalse(result.isDone(), "The response must wait for notification completion");
            } finally {
                release.countDown();
            }
            assertThat(result.get(5, SECONDS))
                .allMatch(EmailNotificationRequestSuccess.class::isInstance)
                .extracting(EmailNotificationRequestStatus::getNotificationRequest)
                .containsExactlyElementsOf(requests);
        }
    }

    @Test
    void shouldReturnFailureWhenExecutorRejectsNotification() {
        RejectedExecutionException rejection = new RejectedExecutionException("Executor stopped");
        Executor rejectingExecutor = task -> {
            throw rejection;
        };
        NotifyService stoppedService = new NotifyService(appParams, notificationClient, rejectingExecutor);
        EmailNotificationRequest request = new EmailNotificationRequest(CASE_ID, TEST_EMAIL);

        List<EmailNotificationRequestStatus> results = stoppedService.sendEmail(List.of(request));

        assertThat(results).singleElement().isInstanceOfSatisfying(EmailNotificationRequestFailure.class,
            failure -> {
                assertThat(failure.getNotificationRequest()).isSameAs(request);
                assertThat(failure.getException()).isSameAs(rejection);
            });
    }

    @Test
    void shouldKeepSuccessfulResultsWhenAnotherNotificationFails() throws NotificationClientException {
        given(appParams.getEmailTemplateId()).willReturn(EMAIL_TEMPLATE_ID);
        SendEmailResponse sendEmailResponse = mock(SendEmailResponse.class);
        given(notificationClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
            .willReturn(sendEmailResponse)
            .willThrow(new NotificationClientException("Notify unavailable"));
        List<EmailNotificationRequest> requests = List.of(
            new EmailNotificationRequest(CASE_ID, TEST_EMAIL),
            new EmailNotificationRequest("12345679", "second@hmcts.net"));

        List<EmailNotificationRequestStatus> results = notifyService.sendEmail(requests);

        assertThat(results).extracting(EmailNotificationRequestStatus::getNotificationRequest)
            .containsExactlyElementsOf(requests);
        assertThat(results.get(0)).isInstanceOf(EmailNotificationRequestSuccess.class);
        assertThat(results.get(1)).isInstanceOf(EmailNotificationRequestFailure.class);
    }
}
