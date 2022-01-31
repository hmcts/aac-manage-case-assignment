package uk.gov.hmcts.reform.managecase.service.ras;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.api.errorhandling.BadRequestException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ResourceNotFoundException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ServiceException;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentQuery;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ROLE_ASSIGNMENTS_CLIENT_ERROR;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ROLE_ASSIGNMENT_SERVICE_ERROR;

class RoleAssignmentServiceHelperTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RoleAssignmentResponse mockedRoleAssignmentResponse;

    private RoleAssignmentServiceHelper roleAssignmentServiceHelper;

    private final List<String> caseIds = Arrays.asList("111", "222");
    private final List<String> userIds = Arrays.asList("111", "222");
    private final List<RoleAssignmentQuery> roleAssignmentQueryList = new ArrayList<>();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        String roleBaseUrl = "roleBaseURL";
        given(applicationParams.amQueryRoleAssignmentsURL()).willReturn(roleBaseUrl);
        String deleteRoleUrl = "deleteRoleUrl";
        given(applicationParams.amDeleteByQueryRoleAssignmentsURL()).willReturn(deleteRoleUrl);
        roleAssignmentServiceHelper = new RoleAssignmentServiceHelperImpl(restTemplate,
                                                                          applicationParams, securityUtils);
    }

    @Test
    void shouldThrow404_FindRoleAssignmentsByCasesAndUsers() {
        Exception exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                                                       eq(RoleAssignmentResponse.class));
        final ResourceNotFoundException expectedException =
            assertThrows(ResourceNotFoundException.class, () -> roleAssignmentServiceHelper
                .findRoleAssignmentsByCasesAndUsers(caseIds, userIds));
        assertEquals("No Role Assignments found for userIds=[111, 222] and casesIds=[111, 222] when getting "
                         + "from Role Assignment Service because of 404 NOT_FOUND", expectedException.getMessage());
    }

    @Test
    void shouldThrow400_FindRoleAssignmentsByCasesAndUsers() {
        Exception exception = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                                                       eq(RoleAssignmentResponse.class));
        final BadRequestException expectedException =
            assertThrows(BadRequestException.class, () -> roleAssignmentServiceHelper
                .findRoleAssignmentsByCasesAndUsers(caseIds, userIds));
        assertEquals("Client error when getting Role Assignments from Role Assignment Service "
                     + "because of 400 BAD_REQUEST", expectedException.getMessage());
    }

    @Test
    void shouldThrow500_FindRoleAssignmentsByCasesAndUsers() {
        Exception exception = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                                                       eq(RoleAssignmentResponse.class));
        final ServiceException expectedException =
            assertThrows(ServiceException.class, () -> roleAssignmentServiceHelper
                .findRoleAssignmentsByCasesAndUsers(caseIds, userIds));
        assertEquals("Problem getting Role Assignments from Role Assignment Service because of "
                     + "500 INTERNAL_SERVER_ERROR", expectedException.getMessage());
    }

    @Test
    void shouldThrow500_deleteRoleAssignmentsByQuery() {
        Exception exception = new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                                                       eq(Void.class));
        final ServiceException actualException =
            assertThrows(ServiceException.class, () -> roleAssignmentServiceHelper
                .deleteRoleAssignmentsByQuery(roleAssignmentQueryList));
        assertEquals(String.format(ROLE_ASSIGNMENT_SERVICE_ERROR, "deleting", HttpStatus.INTERNAL_SERVER_ERROR),
                     actualException.getMessage());
    }

    @Test
    void shouldThrow400_deleteRoleAssignmentsByQuery() {
        Exception exception = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        doThrow(exception).when(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                                                       eq(Void.class));
        final BadRequestException actualException =
            assertThrows(BadRequestException.class, () -> roleAssignmentServiceHelper
                .deleteRoleAssignmentsByQuery(roleAssignmentQueryList));
        assertEquals(String.format(ROLE_ASSIGNMENTS_CLIENT_ERROR, "deleting", HttpStatus.BAD_REQUEST),
                     actualException.getMessage());
    }
}
