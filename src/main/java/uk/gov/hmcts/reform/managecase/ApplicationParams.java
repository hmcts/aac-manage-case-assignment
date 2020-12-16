package uk.gov.hmcts.reform.managecase;

import org.springframework.beans.factory.annotation.Value;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

@Named
@Singleton
public class ApplicationParams {

    @Value("${idam.caa.username}")
    private String caaSystemUserId;
    @Value("${idam.caa.password}")
    private String caaSystemUserPassword;
    @Value("${idam.noc-approver.username}")
    private String nocApproverSystemUserId;
    @Value("${idam.noc-approver.password}")
    private String nocApproverPassword;
    @Value("${ccd.data-store.allowed-urls}")
    private List<String> ccdDataStoreAllowedUrls;
    @Value("${ccd.data-store.allowed-service}")
    private String ccdDataStoreAllowedService;
    @Value("${ccd.definition-store.allowed-urls}")
    private List<String> ccdDefinitionStoreAllowedUrls;
    @Value("${ccd.definition-store.allowed-service}")
    private String ccdDefinitionStoreAllowedService;
    @Value("${notify.email-template-id}")
    private String emailTemplateId;
    @Value("${notify.api-key}")
    private String notifyApiKey;

    public String getCaaSystemUserId() {
        return caaSystemUserId;
    }

    public String getCaaSystemUserPassword() {
        return caaSystemUserPassword;
    }

    public String getNocApproverSystemUserId() {
        return nocApproverSystemUserId;
    }

    public String getNocApproverPassword() {
        return nocApproverPassword;
    }

    public List<String> getCcdDataStoreAllowedUrls() {
        return ccdDataStoreAllowedUrls;
    }

    public String getCcdDataStoreAllowedService() {
        return ccdDataStoreAllowedService;
    }

    public List<String> getCcdDefinitionStoreAllowedUrls() {
        return ccdDefinitionStoreAllowedUrls;
    }

    public String getCcdDefinitionStoreAllowedService() {
        return ccdDefinitionStoreAllowedService;
    }

    public String getEmailTemplateId() {
        return emailTemplateId;
    }

    public String getNotifyApiKey() {
        return notifyApiKey;
    }
}
