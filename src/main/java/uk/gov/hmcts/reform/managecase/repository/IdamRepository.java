package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.ApplicationParams;

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

    @Cacheable("systemUserAccessTokenCache")
    public String getSystemUserAccessToken() {
        return idamClient.getAccessToken(appParams.getIdamSystemUserId(), appParams.getIdamSystemUserPassword());
    }

    public UserDetails getUserByUserId(String bearerToken, String userId) {
        return idamClient.getUserByUserId(bearerToken, userId);
    }
}
