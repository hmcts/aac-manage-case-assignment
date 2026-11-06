package uk.gov.hmcts.reform.managecase.service;

import com.google.common.collect.Maps;
import java.net.ConnectException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequest;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequestFailure;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequestStatus;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequestSuccess;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

@Service
public class NotifyService {

    private final NotificationClient notificationClient;
    private final ApplicationParams appParams;
    private final Executor executor;

    @Autowired
    public NotifyService(ApplicationParams appParams,
                         NotificationClient notificationClient,
                         @Qualifier("notificationTaskExecutor") Executor executor) {
        this.notificationClient = notificationClient;
        this.appParams = appParams;
        this.executor = executor;
    }

    public List<EmailNotificationRequestStatus> sendEmail(final List<EmailNotificationRequest> notificationRequests) {
        if (notificationRequests == null || notificationRequests.isEmpty()) {
            throw new ValidationException("At least one email notification request is required to send notification");
        }

        List<CompletableFuture<EmailNotificationRequestStatus>> futures = notificationRequests.stream()
            .map(this::sendAsync)
            .toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private CompletableFuture<EmailNotificationRequestStatus> sendAsync(EmailNotificationRequest request) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return sendNotification(request);
                } catch (Exception e) {
                    return new EmailNotificationRequestFailure(request, e);
                }
            }, executor);
        } catch (RejectedExecutionException e) {
            return CompletableFuture.completedFuture(new EmailNotificationRequestFailure(request, e));
        }
    }

    @Retryable(retryFor = {ConnectException.class}, backoff = @Backoff(delay = 1000, multiplier = 3))
    private EmailNotificationRequestSuccess sendNotification(EmailNotificationRequest request)
        throws NotificationClientException {
        validateRequest(request);
        SendEmailResponse response = this.notificationClient.sendEmail(
            this.appParams.getEmailTemplateId(),
            request.getEmailAddress(),
            personalisationParams(request.getCaseId()),
            createReference()
        );
        return new EmailNotificationRequestSuccess(request, response);
    }

    private void validateRequest(EmailNotificationRequest request) {
        if (request.getCaseId() == null || request.getCaseId().isEmpty()) {
            throw new ValidationException("case id is required to send notification");
        }
        if (request.getEmailAddress() == null || request.getEmailAddress().isEmpty()) {
            throw new ValidationException("email address is required to send notification");
        }
    }

    private Map<String, ?> personalisationParams(final String caseId) {
        Map<String, String> parameters = Maps.newHashMap();
        parameters.put("case_id", caseId);
        return parameters;
    }

    private String createReference() {
        return UUID.randomUUID().toString();
    }
}
