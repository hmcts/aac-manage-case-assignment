package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.ApplicationParams;

import java.util.List;

@Component
public class IdamRepository {

    private final IdamClient idamClient;
    private final ApplicationParams appParams;

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
        return idamClient.getAccessToken(appParams.getCaaSystemUserId(), appParams.getCaaSystemUserPassword());
    }

    @Cacheable("nocApproverAccessTokenCache")
    public String getNocApproverSystemUserAccessToken() {
        return idamClient.getAccessToken(appParams.getNocApproverSystemUserId(), appParams.getNocApproverPassword());
    }

    public UserDetails getUserByUserId(String userId, String bearerToken) {
        return idamClient.getUserByUserId(bearerToken, userId);
    }
}

