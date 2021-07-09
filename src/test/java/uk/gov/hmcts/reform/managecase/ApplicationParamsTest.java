package uk.gov.hmcts.reform.managecase;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationParamsTest {

    private ApplicationParams applicationParams = new ApplicationParams();

    @Test
    public void shouldGetRoleAssignmentServiceHost() {
        final var roleAssignmentServiceHost = "test-value";
        final var baseUrl = roleAssignmentServiceHost + "/am/role-assignments";

        ReflectionTestUtils.setField(applicationParams, "roleAssignmentServiceHost", roleAssignmentServiceHost);

        assertEquals(baseUrl, applicationParams.roleAssignmentBaseURL());

    }

}
