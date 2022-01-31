package uk.gov.hmcts.reform.managecase.data.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.reform.managecase.api.payload.IdamUser;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;

@Service
@Qualifier(CachedUserRepository.QUALIFIER)
@RequestScope
public class CachedUserRepository implements UserRepository {

    public static final String QUALIFIER = "cached";
    private final UserRepository userRepository;
    private final Map<String, Set<String>> userRoles = newHashMap();
    private Optional<String> userName = Optional.empty();

    @Autowired
    public CachedUserRepository(@Qualifier(DefaultUserRepository.QUALIFIER) UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public IdamUser getUser() {
        return userRepository.getUser();
    }

    @Override
    public String getUserId() {
        return userName.orElseGet(() -> {
            userName = Optional.of(userRepository.getUserId());
            return userName.get();
        });
    }

    @Override
    public Set<String> getUserRoles() {
        return userRoles.computeIfAbsent("userRoles", e -> userRepository.getUserRoles());
    }

    @Override
    public boolean anyRoleEqualsAnyOf(List<String> userRoles) {
        return userRepository.anyRoleEqualsAnyOf(userRoles);
    }

}
