package uk.gov.hmcts.reform.managecase.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseCouldNotBeFoundException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseRoleAccessException;
import uk.gov.hmcts.reform.managecase.api.payload.OrganisationAssignedUsersResetRequest;
import uk.gov.hmcts.reform.managecase.domain.OrganisationAssignedUsersCountData;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.OrganisationAssignedUsersService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_NOT_FOUND;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION;

public class OrganisationAssignedUsersControllerTest {

    private static final String CASE_ID_GOOD = "4444333322221111";
    private static final String CASE_ID_NOT_FOUND = "1111222233334444";

    private static final String CLIENT_S2S_TOKEN = "s2s_token";

    private static final String S2S_SERVICE_GOOD = "S2S_SERVICE_GOOD";
    private static final String S2S_SERVICE_BAD = "S2S_SERVICE_BAD";

    @Mock
    private OrganisationAssignedUsersService organisationAssignedUsersService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    OrganisationAssignedUsersController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ReflectionTestUtils.setField(
            controller,
            "authorisedServicesForOrganisationAssignedUsers",
            List.of(S2S_SERVICE_GOOD)
        );
    }

    @Nested
    class RestOrganisationCountForCase {

        @DisplayName("Should verify S2S token and throw exception when service not authorised")
        @Test
        void shouldVerifyS2SToken_throwExceptionWhenNotAuthorised() {

            // GIVEN
            setUpBadS2sToken();

            // WHEN
            CaseRoleAccessException exception = assertThrows(
                CaseRoleAccessException.class, () -> controller.restOrganisationCountForCase(CLIENT_S2S_TOKEN,
                                                                                             CASE_ID_GOOD,
                                                                                             true));

            // THEN
            verify(securityUtils).getServiceNameFromS2SToken(CLIENT_S2S_TOKEN);
            assertThat(exception.getMessage(), containsString(CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION));

        }

        @DisplayName("Should verify S2S token and when authorised pass data to service")
        @Test
        void shouldVerifyS2SToken_thenPassDataToService() {

            // GIVEN
            setUpGoodS2sToken();

            // WHEN
            controller.restOrganisationCountForCase(CLIENT_S2S_TOKEN, CASE_ID_GOOD, true);

            // THEN
            verify(securityUtils).getServiceNameFromS2SToken(CLIENT_S2S_TOKEN);
            verify(organisationAssignedUsersService).calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD);

        }

        @DisplayName("should call service then if dry run skip save")
        @Test
        void shouldCallService_thenIfDryRunSkipSave() {

            // GIVEN
            setUpGoodS2sToken();
            OrganisationAssignedUsersCountData dataFromService = generateOrgCountWithResults();
            when(organisationAssignedUsersService.calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD))
                .thenReturn(dataFromService);

            // WHEN
            controller.restOrganisationCountForCase(CLIENT_S2S_TOKEN, CASE_ID_GOOD, true);

            // THEN
            verify(organisationAssignedUsersService).calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD);
            verify(organisationAssignedUsersService, never()).saveOrganisationUserCount(any());

        }

        @DisplayName("should call service then if NOT dry run should call save if org count data returned")
        @Test
        void shouldCallService_thenIfNotDryRun_callSaveIfOrgCountDataReturned() {

            // GIVEN
            setUpGoodS2sToken();
            OrganisationAssignedUsersCountData dataFromService = generateOrgCountWithResults();
            when(organisationAssignedUsersService.calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD))
                .thenReturn(dataFromService);

            // WHEN
            controller.restOrganisationCountForCase(CLIENT_S2S_TOKEN, CASE_ID_GOOD, false);

            // THEN
            verify(organisationAssignedUsersService).calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD);
            verify(organisationAssignedUsersService).saveOrganisationUserCount(dataFromService);

        }

        @ParameterizedTest(
            name = "should call service then if NOT dry run should skip save if NO org count data returned: {0}"
        )
        @NullAndEmptySource
        void shouldCallService_thenIfNotDryRun_skipSaveIfNoOrgCountDataReturned(Map<String, Long> orgAssignedUsers) {

            // GIVEN
            setUpGoodS2sToken();
            OrganisationAssignedUsersCountData dataFromService = OrganisationAssignedUsersCountData.builder()
                .caseId(CASE_ID_GOOD)
                .orgsAssignedUsers(orgAssignedUsers)
                .build();
            when(organisationAssignedUsersService.calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD))
                .thenReturn(dataFromService);

            // WHEN
            controller.restOrganisationCountForCase(CLIENT_S2S_TOKEN, CASE_ID_GOOD, false);

            // THEN
            verify(organisationAssignedUsersService).calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD);
            verify(organisationAssignedUsersService, never()).saveOrganisationUserCount(dataFromService);

        }

        @DisplayName("Should return data from service")
        @Test
        void shouldReturnDataFromService() {

            // GIVEN
            setUpGoodS2sToken();
            OrganisationAssignedUsersCountData dataFromService = generateOrgCountWithResults();
            when(organisationAssignedUsersService.calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD))
                .thenReturn(dataFromService);

            // WHEN
            var response = controller.restOrganisationCountForCase(CLIENT_S2S_TOKEN, CASE_ID_GOOD, true);

            // THEN
            assertEquals(dataFromService, response);

        }

    }

    @Nested
    class RestOrganisationCountForMultipleCases {

        @DisplayName("Should verify S2S token and throw exception when service not authorised")
        @Test
        void shouldVerifyS2SToken_throwExceptionWhenNotAuthorised() {

            // GIVEN
            setUpBadS2sToken();

            OrganisationAssignedUsersResetRequest request = new OrganisationAssignedUsersResetRequest(
                List.of(CASE_ID_GOOD),
                true
            );

            // WHEN
            CaseRoleAccessException exception = assertThrows(
                CaseRoleAccessException.class, () -> controller.restOrganisationCountForMultipleCases(CLIENT_S2S_TOKEN,
                                                                                                      request));

            // THEN
            verify(securityUtils).getServiceNameFromS2SToken(CLIENT_S2S_TOKEN);
            assertThat(exception.getMessage(), containsString(CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION));

        }

        @DisplayName("Should verify S2S token and when authorised pass data to service")
        @Test
        void shouldVerifyS2SToken_thenPassDataToService() {

            // GIVEN
            setUpGoodS2sToken();

            OrganisationAssignedUsersResetRequest request = new OrganisationAssignedUsersResetRequest(
                List.of(CASE_ID_GOOD),
                true
            );

            // WHEN
            controller.restOrganisationCountForMultipleCases(CLIENT_S2S_TOKEN, request);

            // THEN
            verify(securityUtils).getServiceNameFromS2SToken(CLIENT_S2S_TOKEN);
            verify(organisationAssignedUsersService).calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD);

        }

        @DisplayName("should call service then if dry run skip save")
        @Test
        void shouldCallService_thenIfDryRunSkipSave() {

            // GIVEN
            setUpGoodS2sToken();

            OrganisationAssignedUsersResetRequest request = new OrganisationAssignedUsersResetRequest(
                List.of(CASE_ID_GOOD),
                true
            );

            OrganisationAssignedUsersCountData dataFromService = generateOrgCountWithResults();
            when(organisationAssignedUsersService.calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD))
                .thenReturn(dataFromService);

            // WHEN
            controller.restOrganisationCountForMultipleCases(CLIENT_S2S_TOKEN, request);

            // THEN
            verify(organisationAssignedUsersService).calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD);
            verify(organisationAssignedUsersService, never()).saveOrganisationUserCount(any());

        }

        @DisplayName("should call service then if NOT dry run should call save if org count data returned")
        @Test
        void shouldCallService_thenIfNotDryRun_callSaveIfOrgCountDataReturned() {

            // GIVEN
            setUpGoodS2sToken();

            OrganisationAssignedUsersResetRequest request = new OrganisationAssignedUsersResetRequest(
                List.of(CASE_ID_GOOD),
                false
            );

            OrganisationAssignedUsersCountData dataFromService = generateOrgCountWithResults();
            when(organisationAssignedUsersService.calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD))
                .thenReturn(dataFromService);

            // WHEN
            controller.restOrganisationCountForMultipleCases(CLIENT_S2S_TOKEN, request);

            // THEN
            verify(organisationAssignedUsersService).calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD);
            verify(organisationAssignedUsersService).saveOrganisationUserCount(dataFromService);

        }

        @ParameterizedTest(
            name = "should call service then if NOT dry run should skip save if NO org count data returned: {0}"
        )
        @NullAndEmptySource
        void shouldCallService_thenIfNotDryRun_skipSaveIfNoOrgCountDataReturned(Map<String, Long> orgAssignedUsers) {

            // GIVEN
            setUpGoodS2sToken();

            OrganisationAssignedUsersResetRequest request = new OrganisationAssignedUsersResetRequest(
                List.of(CASE_ID_GOOD),
                false
            );

            OrganisationAssignedUsersCountData dataFromService = OrganisationAssignedUsersCountData.builder()
                .caseId(CASE_ID_GOOD)
                .orgsAssignedUsers(orgAssignedUsers)
                .build();
            when(organisationAssignedUsersService.calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD))
                .thenReturn(dataFromService);

            // WHEN
            controller.restOrganisationCountForMultipleCases(CLIENT_S2S_TOKEN, request);

            // THEN
            verify(organisationAssignedUsersService).calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD);
            verify(organisationAssignedUsersService, never()).saveOrganisationUserCount(dataFromService);

        }

        @DisplayName("Should return data from service")
        @Test
        void shouldReturnDataFromService() {

            // GIVEN
            setUpGoodS2sToken();

            OrganisationAssignedUsersResetRequest request = new OrganisationAssignedUsersResetRequest(
                List.of(CASE_ID_GOOD),
                true
            );

            OrganisationAssignedUsersCountData dataFromService = generateOrgCountWithResults();
            when(organisationAssignedUsersService.calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD))
                .thenReturn(dataFromService);

            // WHEN
            var response = controller.restOrganisationCountForMultipleCases(CLIENT_S2S_TOKEN, request);

            // THEN
            assertEquals(1, response.getOrgUserCounts().size());
            assertEquals(dataFromService, response.getOrgUserCounts().get(0));

        }

        @DisplayName("Should return error from service")
        @Test
        void shouldReturnErrorFromService() {

            // GIVEN
            setUpGoodS2sToken();

            OrganisationAssignedUsersResetRequest request = new OrganisationAssignedUsersResetRequest(
                List.of(CASE_ID_NOT_FOUND),
                true
            );

            when(organisationAssignedUsersService.calculateOrganisationAssignedUsersCountOnCase(CASE_ID_NOT_FOUND))
                .thenThrow(new CaseCouldNotBeFoundException(CASE_NOT_FOUND));

            // WHEN
            var response = controller.restOrganisationCountForMultipleCases(CLIENT_S2S_TOKEN, request);

            // THEN
            assertEquals(1, response.getOrgUserCounts().size());
            var orgCountData = response.getOrgUserCounts().get(0);
            assertEquals(CASE_ID_NOT_FOUND, orgCountData.getCaseId());
            assertTrue(orgCountData.getError().contains(CASE_NOT_FOUND));

        }

        @DisplayName("Should run for multiple records")
        @Test
        void shouldRunForMultipleRecords() {

            // GIVEN
            setUpGoodS2sToken();

            OrganisationAssignedUsersResetRequest request = new OrganisationAssignedUsersResetRequest(
                List.of(CASE_ID_GOOD, CASE_ID_NOT_FOUND),
                true
            );

            OrganisationAssignedUsersCountData dataFromService1 = generateOrgCountWithResults();
            when(organisationAssignedUsersService.calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD))
                .thenReturn(dataFromService1);
            when(organisationAssignedUsersService.calculateOrganisationAssignedUsersCountOnCase(CASE_ID_NOT_FOUND))
                .thenThrow(new CaseCouldNotBeFoundException(CASE_NOT_FOUND));

            // WHEN
            var response = controller.restOrganisationCountForMultipleCases(CLIENT_S2S_TOKEN, request);

            // THEN
            verify(organisationAssignedUsersService).calculateOrganisationAssignedUsersCountOnCase(CASE_ID_GOOD);
            verify(organisationAssignedUsersService).calculateOrganisationAssignedUsersCountOnCase(CASE_ID_NOT_FOUND);

            assertEquals(2, response.getOrgUserCounts().size());

            var orgCountData1 = response.getOrgUserCounts().stream()
                .filter(data -> CASE_ID_GOOD.equals(data.getCaseId()))
                .findFirst();
            assertTrue(orgCountData1.isPresent());
            assertEquals(dataFromService1, orgCountData1.get());

            var orgCountData2 = response.getOrgUserCounts().stream()
                .filter(data -> CASE_ID_NOT_FOUND.equals(data.getCaseId()))
                .findFirst();
            assertTrue(orgCountData2.isPresent());
            assertEquals(CASE_ID_NOT_FOUND, orgCountData2.get().getCaseId());
            assertTrue(orgCountData2.get().getError().contains(CASE_NOT_FOUND));

        }

    }

    private OrganisationAssignedUsersCountData generateOrgCountWithResults() {
        Map<String, Long> orgAssignedUsers = new HashMap<>();
        orgAssignedUsers.put("OrgId1", 2L);
        return OrganisationAssignedUsersCountData.builder()
            .caseId(CASE_ID_GOOD)
            .orgsAssignedUsers(orgAssignedUsers)
            .build();
    }

    private void setUpBadS2sToken() {
        doReturn(S2S_SERVICE_BAD).when(securityUtils).getServiceNameFromS2SToken(CLIENT_S2S_TOKEN);
    }

    private void setUpGoodS2sToken() {
        doReturn(S2S_SERVICE_GOOD).when(securityUtils).getServiceNameFromS2SToken(CLIENT_S2S_TOKEN);
    }

}