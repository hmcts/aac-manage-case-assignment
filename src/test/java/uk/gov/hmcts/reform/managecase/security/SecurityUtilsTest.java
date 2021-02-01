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
    void hasSolicitorRoleReturnsFalseWhenRolesEmpty() {
        assertFalse(securityUtils.hasSolicitorAndJurisdictionRoles(roles, ""));
    }

    @Test
    void hasSolicitorRoleForJurisdictionReturnsTrueWhenRoleEndsWithSolicitorAndValidCaseWorkerJurisdictionRole() {
        roles.add(CASEWORKER_BEFTA_JURISDICTION_SOLICITOR);
        roles.add(CASEWORKER_BEFTA_JURISDICTION);
        assertTrue(securityUtils.hasSolicitorAndJurisdictionRoles(roles, JURISDICTION));
    }

    @Test
    void hasSolicitorRoleForJurisdictionReturnsTrueWhenRoleEndsWithSolicitorAndContainsJurisdiction() {
        roles.add("caseworker-ia-legalrep-solicitor");
        roles.add("caseworker-ia");
        assertTrue(securityUtils.hasSolicitorAndJurisdictionRoles(roles, "ia"));
    }

    @Test
    void hasSolicitorRoleForJurisdictionReturnsTrueWithMixedCaseRoles() {
        roles.add("caseworker-ia-SoliciTor");
        roles.add("caSewOrker-iA");
        assertTrue(securityUtils.hasSolicitorAndJurisdictionRoles(roles, "ia"));
    }

    @Test
    void hasSolicitorRoleForJurisdictionReturnsFalseWithInvalidSolicitorSuffix() {
        roles.add("caseworker-befta_jurisdiction-barrister");
        roles.add(CASEWORKER_BEFTA_JURISDICTION);
        assertFalse(securityUtils.hasSolicitorAndJurisdictionRoles(roles, JURISDICTION));
    }

    @Test
    void hasSolicitorRoleForJurisdictionReturnsFalseWithInvalidAdditionalSuffixAppended() {
        roles.add("caseworker-befta_jurisdiction-solicitorsurname-solicitor-role");
        roles.add(CASEWORKER_BEFTA_JURISDICTION);
        assertFalse(securityUtils.hasSolicitorAndJurisdictionRoles(roles, JURISDICTION));
    }

    @Test
    void hasSolicitorRoleForJurisdictionReturnsFalseWhenJurisdictionInRoleDoesNotMatch() {
        roles.add("caseworker-ia-legalrep-solicitor");
        roles.add("caseworker-divorce");
        assertFalse(securityUtils.hasSolicitorAndJurisdictionRoles(roles, "ia"));
    }

    @Test
    void hasSolicitorRoleForJurisdictionReturnsFalseWhenJurisdictionSuppliedDoesNotMatch() {
        roles.add("caseworker-ia-legalrep-solicitor");
        roles.add("caseworker-ia");
        assertFalse(securityUtils.hasSolicitorAndJurisdictionRoles(roles, "divorce"));
    }
}
