package uk.gov.hmcts.reform.managecase;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationParamsTest {

    private final ApplicationParams applicationParams = new ApplicationParams();

    @Test
    void shouldGetRoleAssignmentServiceHost() {
        final var roleAssignmentServiceHost = "test-value";
        final var baseUrl = roleAssignmentServiceHost + "/am/role-assignments";

        ReflectionTestUtils.setField(applicationParams, "roleAssignmentServiceHost", roleAssignmentServiceHost);

        assertEquals(baseUrl, applicationParams.roleAssignmentBaseURL());

    }

    @Test
    void shouldGetAmQueryRoleAssignmentsURL() {
        final var roleAssignmentServiceHost = "test-value";
        final var baseUrl = roleAssignmentServiceHost + "/am/role-assignments/query";

        ReflectionTestUtils.setField(applicationParams, "roleAssignmentServiceHost", roleAssignmentServiceHost);

        assertEquals(baseUrl, applicationParams.amQueryRoleAssignmentsURL());

    }


    @Test
    void shouldGetAmDeleteByQueryRoleAssignmentsURL() {
        final var roleAssignmentServiceHost = "test-value";
        final var baseUrl = roleAssignmentServiceHost + "/am/role-assignments/query/delete";

        ReflectionTestUtils.setField(applicationParams, "roleAssignmentServiceHost", roleAssignmentServiceHost);

        assertEquals(baseUrl, applicationParams.amDeleteByQueryRoleAssignmentsURL());

    }

    @Test
    void shouldEncodeStringUsingUtf8() {
        assertEquals("case+id", ApplicationParams.encode("case id"));
    }

    @Test
    void shouldGetAmGetRoleAssignmentsURL() {
        final var roleAssignmentServiceHost = "test-host";
        final var baseUrl = roleAssignmentServiceHost + "/am/role-assignments/actors/{uid}";

        ReflectionTestUtils.setField(applicationParams, "roleAssignmentServiceHost", roleAssignmentServiceHost);

        assertEquals(baseUrl, applicationParams.amGetRoleAssignmentsURL());

    }

}
