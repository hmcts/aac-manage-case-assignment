package uk.gov.hmcts.reform.managecase.data.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.api.payload.IdamUser;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class DefaultUserRepositoryTest {

    private static final String ROLE_CASEWORKER = "caseworker";
    private static final String ROLE_CASEWORKER_TEST = "caseworker-test";
    private static final String ROLE_CASEWORKER_CMC = "caseworker-cmc";
    private static final String ROLE_CASEWORKER_CAA = "caseworker-caa";

    @Mock
    private Authentication authentication;

    @Mock
    private ApplicationParams applicationParams;

    @InjectMocks
    private DefaultUserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        initSecurityContext();

        mockUserInfo("userId");
        mockUserInfo("userId",  emptyList());

        when(applicationParams.getAcaAccessControlCrossJurisdictionRoles())
            .thenReturn(singletonList(ROLE_CASEWORKER_CAA));
        when(applicationParams.getAcaAccessControlCaseworkerRoleRegex()).thenReturn("caseworker.+");
    }

    @Nested
    @DisplayName("getUserRoles()")
    class GetUserRoles {

        @Test
        @DisplayName("should return user roles")
        void shouldReturnUserRoles() {
            asCaseworker();

            final Set<String> roles = userRepository.getUserRoles();

            assertAll(
                () -> assertThat(roles, hasSize(3)),
                () -> assertThat(roles, hasItems(ROLE_CASEWORKER, ROLE_CASEWORKER_TEST, ROLE_CASEWORKER_CMC))
            );
        }
    }

    @Nested
    @DisplayName("getUser()")
    class GetUser {
        @Test
        void shouldRetrieveUserFromIdam() {
            String userId = "userId";

            IdamUser result = userRepository.getUser();

            assertThat(result.getId(), is(userId));
        }
    }

    private void asCaseworker() {
        doReturn(newAuthorities(ROLE_CASEWORKER, ROLE_CASEWORKER_TEST, ROLE_CASEWORKER_CMC)).when(authentication)
            .getAuthorities();
    }

    private GrantedAuthority newAuthority(String authority) {
        return (GrantedAuthority) () -> authority;
    }

    private Collection<GrantedAuthority> newAuthorities(String... authorities) {
        return Arrays.stream(authorities)
            .map(this::newAuthority)
            .collect(Collectors.toSet());
    }

    private void mockUserInfo(String userId) {
        mockUserInfo(userId, emptyList());
    }

    private void mockUserInfo(String userId, List<String> roles) {
        UserInfo userInfo = UserInfo.builder()
            .uid(userId)
            .roles(roles)
            .build();
        when(securityUtils.getUserInfo()).thenReturn(userInfo);
    }

    private void initSecurityContext() {
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
    }
}
