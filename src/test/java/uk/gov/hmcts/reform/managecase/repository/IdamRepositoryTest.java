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

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("Get access token")
    void shouldGetAccessToken() {
        String userId = "TestUser";
        String password = "aPassword";
        given(appParams.getIdamSystemUserId()).willReturn(userId);
        given(appParams.getIdamSystemUserPassword()).willReturn(password);
        given(idamClient.getAccessToken(userId, password)).willReturn(TEST_BEAR_TOKEN);

        String token = idamRepository.getSystemUserAccessToken();

        assertThat(token).isEqualTo(TEST_BEAR_TOKEN);
    }

    @Test
    @DisplayName("Get User by Id")
    void shouldGetUserByUserId() {
        UserDetails userDetails = UserDetails.builder().id(USER_ID).build();
        given(idamClient.getUserByUserId(TEST_BEAR_TOKEN,USER_ID)).willReturn(userDetails);
        UserDetails result = idamRepository.getUserByUserId(USER_ID, TEST_BEAR_TOKEN);
        assertThat(result).isSameAs(userDetails);
    }
}

