package uk.gov.hmcts.reform.managecase.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

class IdamRepositoryTest {

    @Mock
    private IdamClient idamClient;

    @InjectMocks
    private IdamRepository idamRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("Get user info if token is passed")
    void shouldGetUserInfo() {
        UserInfo userInfo = mock(UserInfo.class);
        Mockito.when(idamClient.getUserInfo(anyString())).thenReturn(userInfo);
        UserInfo returnedUserInfo = idamRepository.getUserInfo("Test");
        assertNotNull(returnedUserInfo,"userinfo must not be null");
    }
}

