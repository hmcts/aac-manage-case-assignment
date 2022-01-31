package uk.gov.hmcts.reform.managecase.data.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserRepositoryTest {

    private static final String CASEWORKER_PROBATE_LOA1 = "caseworker-probate-loa1";
    private static final String CASEWORKER_PROBATE_LOA2 = "caseworker-probate-loa2";
    private static final String CASEWORKER_DIVORCE = "caseworker-divorce-loa3";

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        userRepository = spy(new DefaultUserRepository(securityUtils));
    }

    @Nested
    class GetUserRoles {

        @Test
        void shouldRetrieveRolesFromPrincipal() {
            asCaseworker();
            mockUserInfo("userId");

            Set<String> userRoles = userRepository.getUserRoles();

            verify(securityContext, times(1)).getAuthentication();
            verify(authentication, times(1)).getAuthorities();
            assertThat(userRoles, hasItems(CASEWORKER_PROBATE_LOA1, CASEWORKER_PROBATE_LOA2, CASEWORKER_DIVORCE));
        }

        @Test
        void shouldRetrieveNoRoleIfNoRelevantRoleFound() {
            mockUserInfo("userId");

            Set<String> userRoles = userRepository.getUserRoles();

            assertThat(userRoles, is(emptyCollectionOf(String.class)));
        }

    }

    private void mockUserInfo(String userId) {
        UserInfo userInfo = UserInfo.builder()
            .uid(userId)
            .build();
        when(securityUtils.getUserInfo()).thenReturn(userInfo);
    }

    private void asCaseworker() {
        doReturn(newAuthorities(CASEWORKER_PROBATE_LOA1, CASEWORKER_PROBATE_LOA2,
                                CASEWORKER_DIVORCE
        )).when(authentication)
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

}
