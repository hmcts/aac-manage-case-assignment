package uk.gov.hmcts.reform.managecase.service.cau.rolevalidator;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.data.user.UserRepository;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DefaultCaseAssignedUserRoleValidatorTest {

    private final String roleCaseworkerCaa = "caseworker-caa";
    private final String roleCaseworkerSolicitor = "caseworker-solicitor";

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationParams applicationParams;

    private DefaultCaseAssignedUserRoleValidator caseAssignedUserRoleValidator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        caseAssignedUserRoleValidator = new DefaultCaseAssignedUserRoleValidator(userRepository, applicationParams);
    }

    @Test
    void canAccessUserCaseRolesWhenUserRolesContainsValidAccessRole() {
        when(userRepository.anyRoleEqualsAnyOf(any())).thenReturn(true);
        when(applicationParams.getAcaAccessControlCrossJurisdictionRoles())
            .thenReturn(Arrays.asList(roleCaseworkerCaa));
        boolean canAccess = caseAssignedUserRoleValidator.canAccessUserCaseRoles(Lists.newArrayList());
        assertTrue(canAccess);
    }

    @Test
    void canAccessSelfUserCaseRolesWhenSelfUserIdPassedMoreThanOnce() {
        when(userRepository.getUserId()).thenReturn("1234567");
        boolean canAccess = caseAssignedUserRoleValidator.canAccessUserCaseRoles(
            Lists.newArrayList("1234567", "1234567"));
        assertTrue(canAccess);
    }

    @Test
    void canNotAccessOtherUserCaseRolesWhenMoreUserIdsPassedOtherThanSelf() {
        when(userRepository.getUserId()).thenReturn("1234567");
        boolean canAccess = caseAssignedUserRoleValidator.canAccessUserCaseRoles(
            Lists.newArrayList("1234567", "1234568"));
        assertFalse(canAccess);
    }

    @Test
    void canNotAccessOtherUserCaseRolesWhenSelfUserIdNotPassed() {
        when(userRepository.getUserId()).thenReturn("1234567");
        boolean canAccess = caseAssignedUserRoleValidator.canAccessUserCaseRoles(Lists.newArrayList("1234568"));
        assertFalse(canAccess);
    }
}
