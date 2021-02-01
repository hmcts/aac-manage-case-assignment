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
        assertFalse(securityUtils.hasSolicitorRoleForJurisdiction(roles, ""));
    }

    @Test
    void hasSolicitorRoleReturnsTrueWhenRoleEndsWithSolicitor() {
        roles.add(CASEWORKER_BEFTA_JURISDICTION_SOLICITOR);
        assertTrue(securityUtils.hasSolicitorRoleForJurisdiction(roles, JURISDICTION));
    }

    @Test
    void hasSolicitorRoleReturnsTrueWithMixedCase() {
        roles.add("caseworker-ia-SoliciTor");
        assertTrue(securityUtils.hasSolicitorRoleForJurisdiction(roles, "ia"));
    }

    @Test
    void hasSolicitorRoleReturnsTrueWhenRoleEndsWithSolicitorAndContainsJurisdiction() {
        roles.add("caseworker-ia-legalrep-solicitor");
        assertTrue(securityUtils.hasSolicitorRoleForJurisdiction(roles, "ia"));
    }

    @Test
    void hasSolicitorRoleReturnsTrueWhenRoleEndsWithSolicitorAndContainsJurisdictionWithMixedCase() {
        roles.add("caseworker-Ia-legalrep-SoliciTor");
        assertTrue(securityUtils.hasSolicitorRoleForJurisdiction(roles, "ia"));
    }


    @Test
    void hasSolicitorRoleReturnsFalseWithInvalidSuffix() {
        roles.add("caseworker-befta_jurisdiction-barrister");
        assertFalse(securityUtils.hasSolicitorRoleForJurisdiction(roles, JURISDICTION));
    }

    @Test
    void hasSolicitorRoleReturnsFalseWithAdditionalSuffixAppended() {
        roles.add("caseworker-befta_jurisdiction-solicitorsurname-solicitor-role");
        assertFalse(securityUtils.hasSolicitorRoleForJurisdiction(roles, JURISDICTION));
    }
}
