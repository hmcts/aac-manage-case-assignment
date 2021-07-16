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
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

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

    private List<String> caseIds = Arrays.asList(new String[]{"111", "222"});
    private List<String> userIds = Arrays.asList(new String[]{"111", "222"});
    private String roleBaseUrl = "roleBaseURL";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        given(applicationParams.amQueryRoleAssignmentsURL()).willReturn(roleBaseUrl);
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

}
