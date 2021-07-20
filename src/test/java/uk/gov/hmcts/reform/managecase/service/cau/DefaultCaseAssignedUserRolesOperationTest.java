package uk.gov.hmcts.reform.managecase.service.cau;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class DefaultCaseAssignedUserRolesOperationTest {

    private DefaultCaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    @Mock
    private CaseAccessOperation caseAccessOperation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        caseAssignedUserRolesOperation = new DefaultCaseAssignedUserRolesOperation(caseAccessOperation);
    }

    @Test
    void findCaseUserRoles() {
        when(caseAssignedUserRolesOperation.findCaseUserRoles(anyList(), anyList())).thenReturn(
            createCaseAssignedUserRoles());

        List<CaseAssignedUserRole> caseAssignedUserRoles = caseAssignedUserRolesOperation
            .findCaseUserRoles(Lists.newArrayList(), Lists.newArrayList());

        assertEquals(2, caseAssignedUserRoles.size());
    }

    private List<CaseAssignedUserRole> createCaseAssignedUserRoles() {
        List<CaseAssignedUserRole> caseAssignedUserRoles = Lists.newArrayList();

        caseAssignedUserRoles.add(new CaseAssignedUserRole());
        caseAssignedUserRoles.add(new CaseAssignedUserRole());
        return caseAssignedUserRoles;
    }

}
