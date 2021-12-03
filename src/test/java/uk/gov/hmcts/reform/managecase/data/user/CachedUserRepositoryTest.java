package uk.gov.hmcts.reform.managecase.data.user;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CachedUserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    private CachedUserRepository cachedUserRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cachedUserRepository = new CachedUserRepository(userRepository);
    }

    @Nested
    class GetUserId {

        @Test
        @DisplayName("should initially retrieve user name from decorated repository")
        void shouldRetrieveUserNameFromDecorated() {
            String expectedUserName = "26";
            doReturn(expectedUserName).when(userRepository).getUserId();

            String userName = cachedUserRepository.getUserId();

            assertAll(
                () -> assertThat(userName, is(expectedUserName)),
                () -> verify(userRepository, times(1)).getUserId()
            );
        }
    }

    @Nested
    class GetUserRoles {

        @Test
        @DisplayName("should initially retrieve user roles from decorated repository")
        void shouldRetrieveUserRolesFromDecorated() {
            final HashSet<String> expectedUserRoles = Sets.newHashSet("role1", "role2");
            doReturn(expectedUserRoles).when(userRepository).getUserRoles();

            final Set<String> userRoles = cachedUserRepository.getUserRoles();

            assertAll(
                () -> assertThat(userRoles, is(expectedUserRoles)),
                () -> verify(userRepository, times(1)).getUserRoles()
            );
        }
    }

}
