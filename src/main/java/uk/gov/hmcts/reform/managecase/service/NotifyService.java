package uk.gov.hmcts.reform.managecase.service;

import com.google.common.collect.Maps;
import java.net.ConnectException;
import java.util.List;
import java.util.Map;
import javax.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.domain.EmailNotificationFailure;
import uk.gov.hmcts.reform.managecase.domain.EmailNotificationResponse;
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

    public EmailNotificationResponse senEmail(final List<String> caseIds,
                                              final List<String> emailAddresses)  {
        if (caseIds == null || caseIds.isEmpty()) {
            throw new ValidationException("At least one case id is required to send notification");
        }

        if (emailAddresses == null || emailAddresses.isEmpty()) {
            throw new ValidationException("At least one email address is required to send notification");
        }

        EmailNotificationResponse emailNotificationResponse = new EmailNotificationResponse();
        for (String caseId : caseIds) {
            for (String emailAddress : emailAddresses) {
                try {
                    emailNotificationResponse.addSuccessResponse(sendNotification(caseId, emailAddress));
                } catch (NotificationClientException e) {
                    EmailNotificationFailure notificationFailure = new EmailNotificationFailure();
                    notificationFailure.setCaseId(caseId);
                    notificationFailure.setEmailAddress(emailAddress);
                    notificationFailure.setErrorMessage(e.getMessage());
                    emailNotificationResponse.addFailureResponse(notificationFailure);
                }
            }
        }
        return emailNotificationResponse;
    }

    @Retryable(value = {ConnectException.class}, backoff = @Backoff(delay = 1000, multiplier = 3))
    private SendEmailResponse sendNotification(String caseId, String emailAddress) throws NotificationClientException {
        return this.notificationClient.sendEmail(
            this.appParams.getEmailTemplateId(),
            emailAddress,
            personalisationParams(caseId),
            createReference()
        );
    }

    private Map<String, ?> personalisationParams(final String caseId) {
        Map<String, String> parameters = Maps.newHashMap();
        parameters.put("case_id", caseId);
        return parameters;
    }

    private String createReference() {
        return "TestReference";
    }
}
