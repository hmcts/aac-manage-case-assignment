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
    @Value("${ccd.data-store.whitelisted-urls}")
    private List<String> ccdDataStoreWhitelistedUrls;

    public String getIdamSystemUserId() {
        return idamSystemUserId;
    }

    public String getIdamSystemUserPassword() {
        return idamSystemUserPassword;
    }

    public List<String> getCcdDataStoreWhitelistedUrls() {
        return ccdDataStoreWhitelistedUrls;
    }
}

