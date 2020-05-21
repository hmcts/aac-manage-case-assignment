package uk.gov.hmcts.reform.managecase.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtGrantedAuthoritiesConverterTest {

    private static final String ACCESS_TOKEN = "access_token";

    @Mock
    private IdamRepository idamRepository;

    @InjectMocks
    private JwtGrantedAuthoritiesConverter converter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("No Claims should return empty authorities")
    void shouldReturnEmptyAuthoritiesWhenClaimNotAvailable() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.containsClaim(anyString())).thenReturn(false);
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertEquals(0, authorities.size(), "size must be empty");
    }

    @Test
    @DisplayName("Should return empty authorities when claim value is not access_token")
    void shouldReturnEmptyAuthoritiesWhenClaimValueNotEquals() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.containsClaim(anyString())).thenReturn(true);
        when(jwt.getClaim(anyString())).thenReturn("Test");
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertEquals(0, authorities.size(), "size must be empty");
    }

    @Test
    @DisplayName("Should return empty authorities when roles are empty")
    void shouldReturnEmptyAuthoritiesWhenIdamReturnsNoUsers() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.containsClaim(anyString())).thenReturn(true);
        when(jwt.getClaim(anyString())).thenReturn(ACCESS_TOKEN);
        when(jwt.getTokenValue()).thenReturn(ACCESS_TOKEN);
        UserInfo userInfo = mock(UserInfo.class);
        List<String> roles = new ArrayList<>();
        when(userInfo.getRoles()).thenReturn(roles);
        when(idamRepository.getUserInfo(anyString())).thenReturn(userInfo);
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertEquals(0, authorities.size(), "size must be empty");
    }

    @Test
    @DisplayName("Should return authorities as per roles")
    void shouldReturnAuthoritiesWhenIdamReturnsUserRoles() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.containsClaim(anyString())).thenReturn(true);
        when(jwt.getClaim(anyString())).thenReturn(ACCESS_TOKEN);
        when(jwt.getTokenValue()).thenReturn(ACCESS_TOKEN);
        UserInfo userInfo = mock(UserInfo.class);
        List<String> roles = new ArrayList<>();
        roles.add("citizen");
        when(userInfo.getRoles()).thenReturn(roles);
        when(idamRepository.getUserInfo(anyString())).thenReturn(userInfo);
        Collection<GrantedAuthority> authorities = converter.convert(jwt);
        assertEquals(1, authorities.size(), "should return one authority");
    }
}
