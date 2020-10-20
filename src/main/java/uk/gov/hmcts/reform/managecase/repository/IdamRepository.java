package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;

import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.ApplicationParams;

@Component
public class IdamRepository {

    public static final String IDAM_ES_QUERY = "id:\"%s\"";

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

    public UserDetails searchUserById(String userId, String bearerToken) {
        List<UserDetails> users = idamClient.searchUsers(bearerToken, String.format(IDAM_ES_QUERY, userId));
        return users.stream()
                .filter(user -> userId.equalsIgnoreCase(user.getId()))
                .reduce((a, b) -> {
                    throw new IllegalStateException("Multiple users with same IDAM id: " + userId);
                })
                .orElseThrow(
                    () -> new IllegalStateException("User unexpectedly unavailable in IDAM with id: " + userId));
    }

}

