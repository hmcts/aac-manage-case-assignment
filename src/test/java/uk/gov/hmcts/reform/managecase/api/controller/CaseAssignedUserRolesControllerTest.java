package uk.gov.hmcts.reform.managecase.api.controller;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.api.errorhandling.BadRequestException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseRoleAccessException;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRolesRequest;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRolesResource;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRolesResponse;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.cau.CaseAssignedUserRolesOperation;
import uk.gov.hmcts.reform.managecase.service.common.UIDService;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.managecase.api.controller.CaseAssignedUserRolesController.REMOVE_SUCCESS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ROLE_FORMAT_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.EMPTY_CASE_ID_LIST;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.EMPTY_CASE_USER_ROLE_LIST;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.ORGANISATION_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.USER_ID_INVALID;

class CaseAssignedUserRolesControllerTest {

    @Mock
    private UIDService caseReferenceService;

    @Mock
    private CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private SecurityUtils securityUtils;

    private CaseAssignedUserRolesController controller;

    private static final String CASE_ID_GOOD = "4444333322221111";
    private static final String CASE_ID_BAD = "1234";

    private static final String ADD_SERVICE_GOOD = "ADD_SERVICE_GOOD";
    private static final String ADD_SERVICE_BAD = "ADD_SERVICE_BAD";

    private static final String CLIENT_S2S_TOKEN_GOOD = "good_s2s_token";
    private static final String CLIENT_S2S_TOKEN_BAD = "bad_s2s_token";

    private static final String CASE_ROLE_GOOD = "[CASE_ROLE_GOOD]";
    private static final String CASE_ROLE_BAD = "CASE_ROLE_BAD";
    private static final String ORGANISATION_ID_GOOD = "ORGANISATION_ID_GOOD";
    private static final String ORGANISATION_ID_BAD = "";
    private static final String USER_ID_1 = "123";
    private static final String USER_ID_2 = "321";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(caseReferenceService.validateUID(CASE_ID_GOOD)).thenReturn(true);
        when(caseReferenceService.validateUID(CASE_ID_BAD)).thenReturn(false);

        controller = new CaseAssignedUserRolesController(applicationParams, caseReferenceService,
                                                         caseAssignedUserRolesOperation, securityUtils
        );
    }

    @Nested
    class GetCaseUserRoles {

        @BeforeEach
        void setUp() {
            when(caseAssignedUserRolesOperation.findCaseUserRoles(anyList(), anyList()))
                .thenReturn(createCaseAssignedUserRoles());
        }

        private List<CaseAssignedUserRole> createCaseAssignedUserRoles() {
            List<CaseAssignedUserRole> userRoles = Lists.newArrayList();
            userRoles.add(new CaseAssignedUserRole());
            userRoles.add(new CaseAssignedUserRole());
            return userRoles;
        }

        @Test
        void getCaseUserRoles_throwsExceptionWhenNullCaseIdListPassed() {
            Optional<List<String>> optionalUserIds = Optional.of(Lists.newArrayList());

            BadRequestException exception = assertThrows(
                BadRequestException.class, () -> controller.getCaseUserRoles(null, optionalUserIds));

            assertAll(
                () -> assertThat(exception.getMessage(), containsString(EMPTY_CASE_ID_LIST)));
        }

        @Test
        void getCaseUserRoles_throwsExceptionWhenEmptyCaseIdListPassed() {
            List<String> caseIds = Lists.newArrayList();
            Optional<List<String>> optionalUserIds = Optional.of(Lists.newArrayList());

            BadRequestException exception = assertThrows(
                BadRequestException.class, () -> controller.getCaseUserRoles(caseIds, optionalUserIds));

            assertAll(() -> assertThat(exception.getMessage(), containsString(EMPTY_CASE_ID_LIST)));
        }

        @Test
        void getCaseUserRoles_throwsExceptionWhenEmptyCaseIdListContainsInvalidCaseId() {
            List<String> caseIds = Lists.newArrayList(CASE_ID_BAD);
            Optional<List<String>> optionalUserIds = Optional.of(Lists.newArrayList());

            BadRequestException exception = assertThrows(
                BadRequestException.class, () -> controller.getCaseUserRoles(caseIds, optionalUserIds));

            assertAll(() -> assertThat(exception.getMessage(), containsString(CASE_ID_INVALID)));
        }

        @Test
        void getCaseUserRoles_throwsExceptionWhenInvalidUserIdListPassed() {
            List<String> caseIds = Lists.newArrayList(CASE_ID_GOOD);
            Optional<List<String>> optionalUserIds = Optional.of(Lists.newArrayList("8900", "", "89002"));

            BadRequestException exception = assertThrows(
                BadRequestException.class, () -> controller.getCaseUserRoles(caseIds, optionalUserIds));

            assertAll(
                () -> assertThat(exception.getMessage(), containsString(USER_ID_INVALID)));
        }

        @Test
        void getCaseUserRoles_shouldGetResponseWhenCaseIdsAndUserIdsPassed() {
            when(caseReferenceService.validateUID(anyString())).thenReturn(true);
            ResponseEntity<CaseAssignedUserRolesResource> response = controller.getCaseUserRoles(
                Lists.newArrayList(CASE_ID_GOOD), Optional.of(Lists.newArrayList("8900", "89002")));

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().getCaseAssignedUserRoles().size());
        }

        @Test
        void getCaseUserRoles_shouldGetResponseWhenCaseIdsPassed() {
            when(caseReferenceService.validateUID(anyString())).thenReturn(true);
            ResponseEntity<CaseAssignedUserRolesResource> response = controller.getCaseUserRoles(
                Lists.newArrayList(CASE_ID_GOOD), Optional.empty());

            assertNotNull(response);
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().getCaseAssignedUserRoles().size());
        }

    }

    @Nested
    @DisplayName("DELETE /case-users")
    class RemoveCaseUserRoles {

        @BeforeEach
        void setUp() {
            when(applicationParams.getAuthorisedServicesForCaseUserRoles()).thenReturn(List.of(ADD_SERVICE_GOOD));
            doReturn(ADD_SERVICE_GOOD).when(securityUtils).getServiceNameFromS2SToken(CLIENT_S2S_TOKEN_GOOD);
        }

        @Test
        void removeCaseUserRoles_shouldCallRemoveWhenValidSingleGoodCaseUserRoleSupplied() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_GOOD, USER_ID_1, CASE_ROLE_GOOD)
            );

            CaseAssignedUserRolesRequest request = new CaseAssignedUserRolesRequest(caseUserRoles);

            // ACT
            ResponseEntity<CaseAssignedUserRolesResponse> response =
                controller.removeCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, request);

            // ASSERT
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(REMOVE_SUCCESS_MESSAGE, response.getBody().getStatus());
            verify(caseAssignedUserRolesOperation, times(1)).removeCaseUserRoles(caseUserRoles);
        }

        @Test
        void removeCaseUserRoles_shouldCallRemoveWhenValidSingleGoodCaseUserRoleSupplied_withOrganisation() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_GOOD, USER_ID_1, CASE_ROLE_GOOD,
                                                         ORGANISATION_ID_GOOD
                )
            );

            CaseAssignedUserRolesRequest request = new CaseAssignedUserRolesRequest(caseUserRoles);

            // ACT
            ResponseEntity<CaseAssignedUserRolesResponse> response =
                controller.removeCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, request);

            // ASSERT
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(REMOVE_SUCCESS_MESSAGE, response.getBody().getStatus());
            verify(caseAssignedUserRolesOperation, times(1)).removeCaseUserRoles(caseUserRoles);
        }

        @Test
        void removeCaseUserRoles_shouldCallRemoveWhenValidMultipleGoodCaseUserRolesSupplied() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_GOOD, USER_ID_1, CASE_ROLE_GOOD),
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_GOOD, USER_ID_2, CASE_ROLE_GOOD),
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_GOOD, USER_ID_2, CASE_ROLE_GOOD,
                                                         ORGANISATION_ID_GOOD
                )
            );

            CaseAssignedUserRolesRequest request = new CaseAssignedUserRolesRequest(caseUserRoles);

            // ACT
            ResponseEntity<CaseAssignedUserRolesResponse> response =
                controller.removeCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, request);

            // ASSERT
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(REMOVE_SUCCESS_MESSAGE, response.getBody().getStatus());
            verify(caseAssignedUserRolesOperation, times(1)).removeCaseUserRoles(caseUserRoles);
        }

        @Test
        void removeCaseUserRoles_throwsExceptionWhenClientServiceNotAuthorised() {
            // ARRANGE
            doReturn(ADD_SERVICE_BAD).when(securityUtils).getServiceNameFromS2SToken(CLIENT_S2S_TOKEN_BAD);

            // ACT / ASSERT
            CaseRoleAccessException exception = assertThrows(
                CaseRoleAccessException.class,
                () -> controller.removeCaseUserRoles(CLIENT_S2S_TOKEN_BAD, null)
            );

            assertAll(
                () -> assertThat(
                    exception.getMessage(),
                    containsString(CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION)
                )
            );
        }

        @Test
        void removeCaseUserRoles_throwsExceptionWhenNullPassed() {
            // ARRANGE

            // ACT / ASSERT
            BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> controller.removeCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, null)
            );

            assertAll(
                () -> assertThat(
                    exception.getMessage(),
                    containsString(EMPTY_CASE_USER_ROLE_LIST)
                )
            );
        }

        @Test
        void removeCaseUserRoles_throwsExceptionWhenNullCaseUserRolesListPassed() {
            // ARRANGE
            CaseAssignedUserRolesRequest addCaseUserRolesRequest = new CaseAssignedUserRolesRequest(null);

            // ACT / ASSERT
            BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> controller.removeCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, addCaseUserRolesRequest)
            );

            assertAll(
                () -> assertThat(
                    exception.getMessage(),
                    containsString(EMPTY_CASE_USER_ROLE_LIST)
                )
            );
        }

        @Test
        void removeCaseUserRoles_throwsExceptionWhenEmptyCaseUserRolesListPassed() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList();

            CaseAssignedUserRolesRequest request = new CaseAssignedUserRolesRequest(caseUserRoles);

            // ACT / ASSERT
            BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> controller.removeCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, request)
            );

            assertAll(
                () -> assertThat(
                    exception.getMessage(),
                    containsString(EMPTY_CASE_USER_ROLE_LIST)
                )
            );
        }

        @Test
        void removeCaseUserRoles_throwsExceptionWhenInvalidCaseIdPassed() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                // case_id: has to be a valid 16-digit Luhn number
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_BAD, USER_ID_1, CASE_ROLE_GOOD)
            );

            CaseAssignedUserRolesRequest request = new CaseAssignedUserRolesRequest(caseUserRoles);

            // ACT / ASSERT
            BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> controller.removeCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, request)
            );

            assertAll(
                () -> assertThat(
                    exception.getMessage(),
                    containsString(CASE_ID_INVALID)
                )
            );
        }

        @Test
        void removeCaseUserRoles_throwsExceptionWhenInvalidUserIdPassed() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                // user_id: has to be a string of length > 0
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_GOOD, "", CASE_ROLE_GOOD)
            );

            CaseAssignedUserRolesRequest request = new CaseAssignedUserRolesRequest(caseUserRoles);

            // ACT / ASSERT
            BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> controller.removeCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, request)
            );

            assertAll(
                () -> assertThat(
                    exception.getMessage(),
                    containsString(USER_ID_INVALID)
                )
            );
        }

        @Test
        void removeCaseUserRoles_throwsExceptionWhenInvalidCaseRolePassed() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                // case_role: has to be a none-empty string in square brackets
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_GOOD, "", CASE_ROLE_BAD)
            );

            CaseAssignedUserRolesRequest request = new CaseAssignedUserRolesRequest(caseUserRoles);

            // ACT / ASSERT
            BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> controller.removeCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, request)
            );

            assertAll(
                () -> assertThat(
                    exception.getMessage(),
                    containsString(CASE_ROLE_FORMAT_INVALID)
                )
            );
        }

        @Test
        void removeCaseUserRoles_throwsExceptionWhenInvalidOrganisationIdPassed() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                // organisation_id: has to be a non-empty string, when present
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_GOOD, USER_ID_1, CASE_ROLE_GOOD,
                                                         ORGANISATION_ID_BAD
                )
            );

            CaseAssignedUserRolesRequest request = new CaseAssignedUserRolesRequest(caseUserRoles);

            // ACT / ASSERT
            BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> controller.removeCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, request)
            );

            assertAll(
                () -> assertThat(
                    exception.getMessage(),
                    containsString(ORGANISATION_ID_INVALID)
                )
            );
        }

        @Test
        void removeCaseUserRoles_throwsExceptionWhenMultipleErrorsPassed() {
            // ARRANGE
            List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                // case_id: has to be a valid 16-digit Luhn number
                // case_role: has to be a none-empty string in square brackets
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_BAD, USER_ID_1, CASE_ROLE_BAD),
                // user_id: has to be a string of length > 0
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_GOOD, "", CASE_ROLE_GOOD),
                // organisation_id: has to be a non-empty string, when present
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_GOOD, USER_ID_1, CASE_ROLE_GOOD,
                                                         ORGANISATION_ID_BAD
                )
            );

            CaseAssignedUserRolesRequest request = new CaseAssignedUserRolesRequest(caseUserRoles);

            // ACT / ASSERT
            BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> controller.removeCaseUserRoles(CLIENT_S2S_TOKEN_GOOD, request)
            );

            assertAll(
                () -> assertThat(
                    exception.getMessage(),
                    containsString(CASE_ID_INVALID)
                ),
                () -> assertThat(
                    exception.getMessage(),
                    containsString(USER_ID_INVALID)
                ),
                () -> assertThat(
                    exception.getMessage(),
                    containsString(CASE_ROLE_FORMAT_INVALID)
                ),
                () -> assertThat(
                    exception.getMessage(),
                    containsString(ORGANISATION_ID_INVALID)
                )
            );
        }

    }

}
