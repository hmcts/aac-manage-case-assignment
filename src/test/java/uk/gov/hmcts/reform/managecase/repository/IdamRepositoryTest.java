package uk.gov.hmcts.reform.managecase.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserDetails;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.ApplicationParams;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@SuppressWarnings("PMD.MethodNamingConventions")
class IdamRepositoryTest {

    private static final String TEST_BEAR_TOKEN = "TestBearToken";
    private static final String USER_ID = "232-SFWE-4543-CVDSF";

    @Mock
    private IdamClient idamClient;
    @Mock
    private ApplicationParams appParams;

    @InjectMocks
    private IdamRepository idamRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("Get user info if token is passed")
    void shouldGetUserInfo() {
        UserInfo userInfo = UserInfo.builder().build();
        given(idamClient.getUserInfo(TEST_BEAR_TOKEN)).willReturn(userInfo);
        UserInfo result = idamRepository.getUserInfo(TEST_BEAR_TOKEN);
        assertThat(result).isSameAs(userInfo);
    }

    @Test
    @DisplayName("Get caa access token")
    void shouldGetCaaAccessToken() {
        String userId = "TestCaaUser";
        String password = "aPassword";
        given(appParams.getCaaSystemUserId()).willReturn(userId);
        given(appParams.getCaaSystemUserPassword()).willReturn(password);
        given(idamClient.getAccessToken(userId, password)).willReturn(TEST_BEAR_TOKEN);

        String token = idamRepository.getCaaSystemUserAccessToken();

        assertThat(token).isEqualTo(TEST_BEAR_TOKEN);
    }

    @Test
    @DisplayName("Get noc approver access token")
    void shouldGetNocApproverAccessToken() {
        String userId = "TestNocApprover";
        String password = "aPassword";
        given(appParams.getNocApproverSystemUserId()).willReturn(userId);
        given(appParams.getNocApproverPassword()).willReturn(password);
        given(idamClient.getAccessToken(userId, password)).willReturn(TEST_BEAR_TOKEN);

        String token = idamRepository.getNocApproverSystemUserAccessToken();

        assertThat(token).isEqualTo(TEST_BEAR_TOKEN);
    }

    @Test
    @DisplayName("Search users")
    void shouldSearchUsers() {
        UserDetails userDetails = UserDetails.builder().id(USER_ID).build();
        List<UserDetails> userList = List.of(userDetails);
        given(idamClient.searchUsers(TEST_BEAR_TOKEN, String.format(IdamRepository.IDAM_ES_QUERY, USER_ID)))
                .willReturn(userList);
        UserDetails result = idamRepository.searchUserById(USER_ID, TEST_BEAR_TOKEN);
        assertThat(result).isSameAs(userDetails);
    }

    @Test
    @DisplayName("Search users with duplicate users in the results")
    void shouldThrowIllegalStateException_whenDuplicateUsersReturnForSearch() {
        UserDetails userDetails = UserDetails.builder().id(USER_ID).build();
        List<UserDetails> userList = List.of(userDetails, userDetails);
        given(idamClient.searchUsers(TEST_BEAR_TOKEN, String.format(IdamRepository.IDAM_ES_QUERY, USER_ID)))
                .willReturn(userList);

        assertThatThrownBy(() -> idamRepository.searchUserById(USER_ID, TEST_BEAR_TOKEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple users with same IDAM id");

    }
}

