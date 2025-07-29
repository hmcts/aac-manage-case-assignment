package uk.gov.hmcts.reform.managecase.security;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.JUnitAssertionsShouldIncludeMessage"})
class SecurityUtilsTest {

    private static final String CASEWORKER_BEFTA_JURISDICTION_SOLICITOR = "caseworker-befta_jurisdiction-solicitor";
    private static final String CASEWORKER_BEFTA_JURISDICTION = "caseworker-befta_jurisdiction";
    private static final String JURISDICTION = "befta_jurisdiction";
    private static final String SERVICE_JWT = "7gf364fg367f67";
    private static final String USER_ID = "123";
    private static final String USER_JWT = "8gf364fg367f67";

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private IdamRepository idamRepository;

    @InjectMocks
    private SecurityUtils securityUtils;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private List<String> roles;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        roles = new ArrayList<>();

        final GrantedAuthority[] authorities = new GrantedAuthority[] { newAuthority("role1"), newAuthority("role2")};
        when(authTokenGenerator.generate()).thenReturn(SERVICE_JWT);

        Jwt jwt =   Jwt.withTokenValue(USER_JWT)
            .claim("aClaim", "aClaim")
            .claim("aud", Lists.newArrayList("aac_management"))
            .header("aHeader", "aHeader")
            .build();
        Collection<? extends GrantedAuthority> authorityCollection = Stream.of("role1", "role2")
            .map(a -> new SimpleGrantedAuthority(a))
            .collect(Collectors.toCollection(ArrayList::new));

        doReturn(jwt).when(authentication).getPrincipal();
        doReturn(authentication).when(securityContext).getAuthentication();
        when(authentication.getAuthorities()).thenAnswer(invocationOnMock -> authorityCollection);
        SecurityContextHolder.setContext(securityContext);

        UserInfo userInfo = UserInfo.builder()
            .uid(USER_ID)
            .sub("emailId@a.com")
            .build();
        when(idamRepository.getUserInfo(anyString())).thenReturn(userInfo);
    }

    @Test
    void hasSolicitorAndJurisdictionRolesReturnsFalseWhenRolesEmpty() {
        assertFalse(securityUtils.hasSolicitorAndJurisdictionRoles(roles, ""));
    }

    @Test
    void hasSolicitorAndJurisdictionRolesReturnsTrueWhenRoleEndsWithSolicitorAndValidCaseWorkerJurisdictionRole() {
        roles.add(CASEWORKER_BEFTA_JURISDICTION_SOLICITOR);
        roles.add(CASEWORKER_BEFTA_JURISDICTION);
        assertTrue(securityUtils.hasSolicitorAndJurisdictionRoles(roles, JURISDICTION));
    }

    @Test
    void hasSolicitorAndJurisdictionRolesReturnsTrueWhenRoleEndsWithSolicitorAndContainsJurisdiction() {
        roles.add("caseworker-ia-legalrep-solicitor");
        roles.add("caseworker-ia");
        assertTrue(securityUtils.hasSolicitorAndJurisdictionRoles(roles, "ia"));
    }

    @Test
    void hasSolicitorAndJurisdictionRolesReturnsTrueWithMixedCaseRoles() {
        roles.add("caseworker-ia-SoliciTor");
        roles.add("caSewOrker-iA");
        assertTrue(securityUtils.hasSolicitorAndJurisdictionRoles(roles, "ia"));
    }

    @Test
    void hasSolicitorAndJurisdictionRolesReturnsFalseWithInvalidSolicitorSuffix() {
        roles.add("caseworker-befta_jurisdiction-barrister");
        roles.add(CASEWORKER_BEFTA_JURISDICTION);
        assertFalse(securityUtils.hasSolicitorAndJurisdictionRoles(roles, JURISDICTION));
    }

    @Test
    void hasSolicitorAndJurisdictionRolesReturnsFalseWithInvalidAdditionalSuffixAppended() {
        roles.add("caseworker-befta_jurisdiction-solicitorsurname-solicitor-role");
        roles.add(CASEWORKER_BEFTA_JURISDICTION);
        assertFalse(securityUtils.hasSolicitorAndJurisdictionRoles(roles, JURISDICTION));
    }

    @Test
    void hasSolicitorAndJurisdictionRolesReturnsFalseWhenJurisdictionInRoleDoesNotMatch() {
        roles.add("caseworker-ia-legalrep-solicitor");
        roles.add("caseworker-divorce");
        assertFalse(securityUtils.hasSolicitorAndJurisdictionRoles(roles, "ia"));
    }

    @Test
    void hasSolicitorAndJurisdictionRolesReturnsFalseWhenJurisdictionSuppliedDoesNotMatch() {
        roles.add("caseworker-ia-legalrep-solicitor");
        roles.add("caseworker-ia");
        assertFalse(securityUtils.hasSolicitorAndJurisdictionRoles(roles, "divorce"));
    }

    @Test
    void authorizationHeaders() {
        final HttpHeaders headers = securityUtils.authorizationHeaders();

        assertAll(
            () -> assertHeader(headers, "ServiceAuthorization", SERVICE_JWT),
            () -> assertHeader(headers, "user-id", USER_ID),
            () -> assertHeader(headers, "user-roles", "role1,role2")
        );
    }

    private void assertHeader(HttpHeaders headers, String name, String value) {
        assertThat(headers.get(name), hasSize(1));
        assertThat(headers.get(name).get(0), equalTo(value));
    }

    private GrantedAuthority newAuthority(String authority) {
        return (GrantedAuthority) () -> authority;
    }
}
