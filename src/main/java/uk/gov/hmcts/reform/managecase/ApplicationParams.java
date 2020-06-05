package uk.gov.hmcts.reform.managecase;

import org.springframework.beans.factory.annotation.Value;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class ApplicationParams {

    @Value("${idam.system-user.username}")
    private String idamSystemUserId;
    @Value("${idam.system-user.password}")
    private String idamSystemUserPassword;

    public String getIdamSystemUserId() {
        return idamSystemUserId;
    }

    public String getIdamSystemUserPassword() {
        return idamSystemUserPassword;
    }
}
