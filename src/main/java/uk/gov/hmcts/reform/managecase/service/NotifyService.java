package uk.gov.hmcts.reform.managecase.service;

import com.google.common.collect.Lists;
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

    public List<SendEmailResponse> senEmail(final List<String> caseIds,
                                            final List<String> emailAddresses) throws NotificationClientException  {
        if (caseIds == null || caseIds.isEmpty()) {
            throw new ValidationException("At least one case id is required to send notification");
        }

        if (emailAddresses == null || emailAddresses.isEmpty()) {
            throw new ValidationException("At least one email address is required to send notification");
        }

        List<SendEmailResponse> emailNotificationResponses = Lists.newArrayList();
        for (String caseId : caseIds) {
            for (String emailAddress : emailAddresses) {
                emailNotificationResponses.add(sendNotification(caseId, emailAddress));
            }
        }
        return emailNotificationResponses;
    }

    @Retryable(value = {ConnectException.class}, backoff = @Backoff(delay = 1000, multiplier = 3))
    private SendEmailResponse sendNotification(String caseId, String emailAddress) throws NotificationClientException {
        return this.notificationClient.sendEmail(
            this.appParams.getEmailTemplateId(),
            emailAddress,
            personalisationParams(caseId),
            createReference(),
            this.appParams.getReplyToEmailId()
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
