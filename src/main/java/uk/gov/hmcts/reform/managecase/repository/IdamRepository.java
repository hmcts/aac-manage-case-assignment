package uk.gov.hmcts.reform.managecase.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.ApplicationParams;

@Component
public class IdamRepository {

    private static final Logger LOG = LoggerFactory.getLogger(IdamRepository.class);

    private final IdamClient idamClient;
    private final ApplicationParams appParams;

    private void jcdebug(String message) {
        LOG.info("JCDEBUG: IdamRepository: " + message);
    }

    @Autowired
    public IdamRepository(IdamClient idamClient, ApplicationParams applicationParams) {
        this.idamClient = idamClient;
        this.appParams = applicationParams;
    }

    @Cacheable("userInfoCache")
    public UserInfo getUserInfo(String bearerToken) {
        return idamClient.getUserInfo(bearerToken);
    }

    @Cacheable("caaAccessTokenCache")
    public String getCaaSystemUserAccessToken() {
        String caaUserId = appParams.getCaaSystemUserId();
        String caaPassword = appParams.getCaaSystemUserPassword();
        jcdebug("getCaaSystemUserAccessToken: " + (caaUserId == null ? "NULL" : caaUserId) + "  "
                    + (caaPassword == null ? "NULL" : caaPassword));
        try {
            return idamClient.getAccessToken(appParams.getCaaSystemUserId(), appParams.getCaaSystemUserPassword());
        } catch (Exception e) {
            jcdebug("EXCEPTION: " + e.getMessage());
            throw e;
        }
    }

    @Cacheable("nocApproverAccessTokenCache")
    public String getNocApproverSystemUserAccessToken() {
        return idamClient.getAccessToken(appParams.getNocApproverSystemUserId(), appParams.getNocApproverPassword());
    }

    public UserDetails getUserByUserId(String userId, String bearerToken) {
        return idamClient.getUserByUserId(bearerToken, userId);
    }
}

