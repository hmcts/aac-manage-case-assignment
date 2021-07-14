package uk.gov.hmcts.reform.managecase.service.cau;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentService;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

public class CaseAccessOperationTest {

    @Mock
    private RoleAssignmentService roleAssignmentService;

    @InjectMocks
    private CaseAccessOperation caseAccessOperation;

    private static final Long CASE_REFERENCE = 1234123412341236L;
    private static final Long CASE_NOT_FOUND = 9999999999999999L;
    private static final String CASE_ROLE = "[DEFENDANT]";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Nested()
    class GetCaseAssignUserRoles {

        @Test
        void shouldGetCaseAssignedUserRoles() {
            List<Long> caseReferences = Lists.newArrayList(CASE_REFERENCE);
            when(roleAssignmentService.findRoleAssignmentsByCasesAndUsers(anyList(), anyList()))
                .thenReturn(getCaseAssignedUserRoles());
            List<CaseAssignedUserRole> caseAssignedUserRoles = caseAccessOperation.findCaseUserRoles(
                caseReferences, Lists.newArrayList());

            assertNotNull(caseAssignedUserRoles);
            assertEquals(1, caseAssignedUserRoles.size());
            assertEquals(CASE_ROLE, caseAssignedUserRoles.get(0).getCaseRole());
        }

        @Test
        void shouldReturnEmptyResultOnNonExistingCases() {
            List<Long> caseReferences = Lists.newArrayList(CASE_NOT_FOUND);
            List<CaseAssignedUserRole> caseAssignedUserRoles = caseAccessOperation.findCaseUserRoles(
                caseReferences, Lists.newArrayList());

            assertNotNull(caseAssignedUserRoles);
            assertEquals(0, caseAssignedUserRoles.size());
        }

        private List<CaseAssignedUserRole> getCaseAssignedUserRoles() {
            return Arrays.asList(
                new CaseAssignedUserRole[]{new CaseAssignedUserRole("caseDataId", "userId", CASE_ROLE)}
            );
        }
    }

}
