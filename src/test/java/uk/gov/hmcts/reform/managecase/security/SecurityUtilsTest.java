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
    private static final String CASEWORKER_BEFTA_JURISDICTION = "caseworker-befta_jurisdiction";
    private static final String JURISDICTION = "befta_jurisdiction";

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
}
