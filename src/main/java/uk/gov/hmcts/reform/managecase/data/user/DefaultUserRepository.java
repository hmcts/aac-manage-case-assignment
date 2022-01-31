package uk.gov.hmcts.reform.managecase.data.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.api.payload.IdamUser;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Qualifier(DefaultUserRepository.QUALIFIER)
public class DefaultUserRepository implements UserRepository {

    public static final String QUALIFIER = "default";
    private final SecurityUtils securityUtils;
    private static final Logger LOG = LoggerFactory.getLogger(DefaultUserRepository.class);

    @Autowired
    public DefaultUserRepository(SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    @Override
    public IdamUser getUser() {
        var userInfo = securityUtils.getUserInfo();
        return toIdamUser(userInfo);
    }

    @Override
    public String getUserId() {
        return securityUtils.getUserId();
    }

    @Override
    public boolean anyRoleEqualsAnyOf(List<String> userRoles) {
        return getUserRoles().stream().anyMatch(userRoles::contains);
    }

    @Override
    public Set<String> getUserRoles() {
        LOG.debug("Getting user roles from security context.");

        Collection<? extends GrantedAuthority> authorities =
            SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        Set<String> userRoles = authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());

        String userId = getUser().getId();
        LOG.info("User id from idam: {}. User roles in the security context: {}.", userId, userRoles);

        return userRoles;
    }

    private IdamUser toIdamUser(UserInfo userInfo) {
        var idamUser = new IdamUser();
        idamUser.setId(userInfo.getUid());
        idamUser.setEmail(userInfo.getSub());
        idamUser.setForename(userInfo.getGivenName());
        idamUser.setSurname(userInfo.getFamilyName());
        return idamUser;
    }
}
