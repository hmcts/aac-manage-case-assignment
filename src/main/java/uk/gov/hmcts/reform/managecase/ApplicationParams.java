package uk.gov.hmcts.reform.managecase;

import org.springframework.beans.factory.annotation.Value;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

@Named
@Singleton
public class ApplicationParams {

    @Value("${idam.system-user.username}")
    private String idamSystemUserId;
    @Value("${idam.system-user.password}")
    private String idamSystemUserPassword;
    @Value("${ccd.data-store.allowed-urls}")
    private List<String> ccdDataStoreAllowedUrls;
    @Value("${ccd.data-store.allowed-service}")
    private String ccdDataStoreAllowedService;
    @Value("${prd.allowed-urls}")
    private List<String> prdAllowedUrls;
    @Value("${prd.allowed-service}")
    private String prdAllowedService;

    @Value("${notify.email-template-id}")
    private String emailTemplateId;

    @Value("${notify.api-key}")
    private String notifyApiKey;

    public String getIdamSystemUserId() {
        return idamSystemUserId;
    }

    public String getIdamSystemUserPassword() {
        return idamSystemUserPassword;
    }

    public List<String> getCcdDataStoreAllowedUrls() {
        return ccdDataStoreAllowedUrls;
    }

    public String getCcdDataStoreAllowedService() {
        return ccdDataStoreAllowedService;
    }

    public List<String> getPrdAllowedUrls() {
        return prdAllowedUrls;
    }

    public void setPrdAllowedUrls(List<String> prdAllowedUrls) {
        this.prdAllowedUrls = prdAllowedUrls;
    }

    public String getPrdAllowedService() {
        return prdAllowedService;
    }

    public void setPrdAllowedService(String prdAllowedService) {
        this.prdAllowedService = prdAllowedService;
    }

    public String getEmailTemplateId() {
        return emailTemplateId;
    }

    public String getNotifyApiKey() {
        return notifyApiKey;
    }
}

