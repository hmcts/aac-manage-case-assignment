package uk.gov.hmcts.reform.managecase.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.net.ConnectException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public NotifyService(ApplicationParams appParams,
                         NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
        this.appParams = appParams;
    }

    public List<EmailNotificationRequestStatus> sendEmail(final List<EmailNotificationRequest> notificationRequests) {
        if (notificationRequests == null || notificationRequests.isEmpty()) {
            throw new ValidationException("At least one email notification request is required to send notification");
        }

        List<EmailNotificationRequestStatus> notificationStatuses = Lists.newArrayList();
        notificationRequests.forEach(emailNotificationRequest -> {
            try {
                notificationStatuses.add(sendNotification(emailNotificationRequest));
            } catch (Exception e) {
                notificationStatuses.add(new EmailNotificationRequestFailure(emailNotificationRequest, e));
            }
        });
        return notificationStatuses;
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
