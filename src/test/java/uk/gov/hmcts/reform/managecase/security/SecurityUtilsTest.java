package uk.gov.hmcts.reform.managecase.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.JUnitAssertionsShouldIncludeMessage"})
class SecurityUtilsTest {

    private static final String CASEWORKER_BEFTA_JURISDICTION_SOLICITOR = "caseworker-befta_jurisdiction-solicitor";

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @Mock
    private IdamRepository idamRepository;

    @InjectMocks
    private SecurityUtils securityUtils;

    private List<String> roles;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        roles = new ArrayList<>();
    }

    @Test
    void hasSolicitorRoleReturnsFalseWhenParametersAreEmpty() {
        assertFalse(securityUtils.hasSolicitorRole(roles, ""));
    }

    @Test
    void hasSolicitorRoleReturnsFalseWhenRolesEmpty() {
        assertFalse(securityUtils.hasSolicitorRole(roles,  CASEWORKER_BEFTA_JURISDICTION_SOLICITOR));
    }

    @Test
    void hasSolicitorRoleReturnsFalseWhenJurisdictionNotPresentInRoles() {
        roles.add(CASEWORKER_BEFTA_JURISDICTION_SOLICITOR);
        assertFalse(securityUtils.hasSolicitorRole(roles, "nonmatchedJurisdiction"));
    }

    @Test
    void hasSolicitorRoleReturnsTrueWhenJurisdictionPresentInRoles() {
        roles.add(CASEWORKER_BEFTA_JURISDICTION_SOLICITOR);
        assertTrue(securityUtils.hasSolicitorRole(roles, "befta_jurisdiction"));
    }

    @Test
    void hasSolicitorRoleReturnsTrueWhenJurisdictionPresentInRolesWithExtraInformation() {
        roles.add("caseworker-befta_jurisdiction-solicitor-solicitorsurname");
        assertTrue(securityUtils.hasSolicitorRole(roles, "befta_jurisdiction"));
    }

    @Test
    void hasSolicitorRoleReturnsTrueWithJurisdictionPresentMixedCaseInRoles() {
        roles.add("caseworker-BEFTA_jurisdiction-solicitor");
        assertTrue(securityUtils.hasSolicitorRole(roles, "befta_JURIsdiction"));
    }
}
