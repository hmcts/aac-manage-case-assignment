package uk.gov.hmcts.reform.managecase.service;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.managecase.TestFixtures;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.datastore.SupplementaryDataUpdateRequest;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.domain.OrganisationsAssignedUsersCountData;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentService;
import uk.gov.hmcts.reform.managecase.util.OrganisationPolicyUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CASE_ID;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.organisationPolicy;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.usersByOrganisation;
import static uk.gov.hmcts.reform.managecase.repository.DefaultDataStoreRepository.ORGS_ASSIGNED_USERS_PATH;

@ExtendWith(MockitoExtension.class)
class OrganisationsAssignedUsersServiceTest {

    private static final String ORGANISATION_ID_1 = "Org 1";
    private static final String ORGANISATION_ID_2 = "Org 2";
    private static final String ORGANISATION_ID_BAD_1 = "Org BAD 1";
    private static final String ORGANISATION_ID_BAD_2 = "Org BAD 2";
    private static final String ORGANISATION_ID_BAD_3 = "Org BAD 3";
    private static final String ROLE_1 = "Role 1";
    private static final String ROLE_2 = "Role 2";
    private static final String ROLE_3 = "Role 3";
    private static final String ROLE_4 = "Role 4";
    private static final String ROLE_BAD = "Role BAD";
    private static final String USER_ID_1 = "User 1";
    private static final String USER_ID_2 = "User 2";
    private static final String USER_ID_BAD_1 = "User BAD 1";
    private static final String USER_ID_BAD_2 = "User BAD 2";

    private static final String SYSTEM_USER_TOKEN = "SYSTEM_TOKEN";
    private static final String APPROVER_USER_TOKEN = "APPROVER_USER_TOKEN";

    @Mock
    private DataStoreRepository dataStoreRepository;

    @Mock
    private PrdRepository prdRepository;

    @Mock
    private OrganisationPolicyUtils organisationPolicyUtils;

    @Mock
    private RoleAssignmentService roleAssignmentService;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private DataStoreApiClient dataStoreApi;

    @InjectMocks
    private OrganisationsAssignedUsersService classUnderTest;

    @Nested
    class CalculateOrganisationsAssignedUsersCountData {

        @Captor
        private ArgumentCaptor<List<String>> caseIdsCaptor;

        @Captor
        private ArgumentCaptor<List<String>> userIdsCaptor;

        @Mock
        private CaseDetails caseDetails;

        private List<OrganisationPolicy> policies;

        @BeforeEach
        void setUp() {
            policies = new ArrayList<>();

            when(dataStoreRepository.findCaseByCaseIdAsSystemUserUsingExternalApi(CASE_ID)).thenReturn(caseDetails);

            when(organisationPolicyUtils.findPolicies(caseDetails)).thenReturn(policies);
        }

        @DisplayName("should load caseDetails and extract policies")
        @Test
        void shouldLoadCaseDetailsAndExtractPolicies() {

            // WHEN
            classUnderTest.calculateOrganisationsAssignedUsersCountData(CASE_ID);

            // THEN
            verify(dataStoreRepository).findCaseByCaseIdAsSystemUserUsingExternalApi(CASE_ID);
            verify(organisationPolicyUtils).findPolicies(caseDetails);
        }

        @DisplayName("should extract assigned organisations and lookup their users")
        @Test
        void shouldExtractAssignedOrganisationsAndLookupTheirUsers() {

            // GIVEN
            generateAndRegisterOrganisationPolicy(ORGANISATION_ID_1, ROLE_1);
            generateAndRegisterOrganisationPolicy(ORGANISATION_ID_2, ROLE_2);
            generateAndRegisterOrganisationPolicyUnassigned(ROLE_BAD);
            generateAndRegisterOrganisationPolicy(ORGANISATION_ID_1, ROLE_3); // NB: duplicate org

            setUpNocApproverSystemUserTokenCall();
            registerUserLookupForOrg(ORGANISATION_ID_1, List.of(USER_ID_1));
            registerUserLookupForOrg(ORGANISATION_ID_2, List.of(USER_ID_2));

            // WHEN
            classUnderTest.calculateOrganisationsAssignedUsersCountData(CASE_ID);

            // THEN
            // ... verify all policies checked
            verify(organisationPolicyUtils, times(4))
                .checkIfPolicyHasOrganisationAssigned(any());

            // ... verify only the two lookups for the two orgs
            verify(prdRepository, atMostOnce()).findUsersByOrganisation(APPROVER_USER_TOKEN, ORGANISATION_ID_1);
            verify(prdRepository, atMostOnce()).findUsersByOrganisation(APPROVER_USER_TOKEN, ORGANISATION_ID_2);
            verifyNoMoreInteractions(prdRepository); // i.e. no more lookups
        }

        @DisplayName("should look up role assignments for case + users if found")
        @Test
        void shouldLookUpRoleAssignmentsForCaseAndUsersIfFound() {

            // GIVEN
            generateAndRegisterOrganisationPolicy(ORGANISATION_ID_1, ROLE_1);
            generateAndRegisterOrganisationPolicy(ORGANISATION_ID_2, ROLE_2);

            setUpNocApproverSystemUserTokenCall();
            registerUserLookupForOrg(ORGANISATION_ID_1, List.of(USER_ID_1));
            registerUserLookupForOrg(ORGANISATION_ID_2, List.of(USER_ID_1, USER_ID_2)); // i.e. user 1 both orgs

            // WHEN
            classUnderTest.calculateOrganisationsAssignedUsersCountData(CASE_ID);

            // THEN
            verify(roleAssignmentService).findRoleAssignmentsByCasesAndUsers(
                caseIdsCaptor.capture(),
                userIdsCaptor.capture()
            );
            assertEquals(1, caseIdsCaptor.getValue().size());
            assertTrue(caseIdsCaptor.getValue().contains(CASE_ID));
            assertEquals(2, userIdsCaptor.getValue().size());
            assertTrue(userIdsCaptor.getValue().contains(USER_ID_1));
            assertTrue(userIdsCaptor.getValue().contains(USER_ID_2));
        }

        @DisplayName("should skip roles assignments lookup if no users found")
        @Test
        void shouldSkipRolesAssignmentsLookupIfNoUsersFound() {

            // GIVEN
            generateAndRegisterOrganisationPolicy(ORGANISATION_ID_1, ROLE_1);

            setUpNocApproverSystemUserTokenCall();
            registerUserLookupForOrg(ORGANISATION_ID_1, Collections.emptyList()); // i.e. no users for org

            // WHEN
            OrganisationsAssignedUsersCountData response =
                classUnderTest.calculateOrganisationsAssignedUsersCountData(CASE_ID);

            // THEN
            // ... verify no role assignment lookup made
            verify(roleAssignmentService, never()).findRoleAssignmentsByCasesAndUsers(any(), any());
            // ... verify response has only zero count data to save
            Map<String, Long> orgsAssignedUsers = response.getOrgsAssignedUsers();
            assertEquals(1, orgsAssignedUsers.size());
            assertEquals(0L, orgsAssignedUsers.get(ORGANISATION_ID_1).longValue());
        }

        @DisplayName("should generate count data")
        @Test
        void shouldGenerateCountDataButIgnoreUsersWithoutRolesForTheirOrg() {

            // GIVEN
            generateAndRegisterOrganisationPolicy(ORGANISATION_ID_1, ROLE_1);
            generateAndRegisterOrganisationPolicy(ORGANISATION_ID_2, ROLE_2);
            generateAndRegisterOrganisationPolicyUnassigned(ROLE_BAD);
            generateAndRegisterOrganisationPolicy(ORGANISATION_ID_1, ROLE_3); // NB: duplicate org

            setUpNocApproverSystemUserTokenCall();
            registerUserLookupForOrg(ORGANISATION_ID_1, List.of(USER_ID_1, USER_ID_2, USER_ID_BAD_1));
            registerUserLookupForOrg(ORGANISATION_ID_2, List.of(USER_ID_1, USER_ID_BAD_2)); // i.e. user 1 both orgs

            when(roleAssignmentService.findRoleAssignmentsByCasesAndUsers(any(), any())).thenReturn(
                List.of(
                    // USER 1: in Org 1 & Org 2 and has org roles for both
                    new CaseAssignedUserRole(CASE_ID, USER_ID_1, ROLE_1),
                    new CaseAssignedUserRole(CASE_ID, USER_ID_1, ROLE_2),
                    new CaseAssignedUserRole(CASE_ID, USER_ID_1, ROLE_3),
                    // USER 2: in Org 1 and has org roles
                    new CaseAssignedUserRole(CASE_ID, USER_ID_2, ROLE_3),
                    // USER BAD 1: in org 1 but has a non-org role :: should be ignored
                    new CaseAssignedUserRole(CASE_ID, USER_ID_BAD_1, ROLE_BAD),
                    // USER BAD 2: in org 2 but has an org1 role :: should be ignored
                    new CaseAssignedUserRole(CASE_ID, USER_ID_BAD_2, ROLE_1)
                )
            );

            when(organisationPolicyUtils.findRolesForOrg(policies, ORGANISATION_ID_1)).thenReturn(
                List.of(ROLE_1, ROLE_3)
            );
            when(organisationPolicyUtils.findRolesForOrg(policies, ORGANISATION_ID_2)).thenReturn(
                List.of(ROLE_2)
            );

            // WHEN
            OrganisationsAssignedUsersCountData response =
                classUnderTest.calculateOrganisationsAssignedUsersCountData(CASE_ID);

            // THEN
            assertNotNull(response);
            assertEquals(CASE_ID, response.getCaseId());

            Map<String, Long> orgsAssignedUsers = response.getOrgsAssignedUsers();
            assertEquals(2, orgsAssignedUsers.size());
            // ORG 1 = 2: 3 in org, but only 2 of those with an org role
            assertEquals(2L, orgsAssignedUsers.get(ORGANISATION_ID_1).longValue());
            // ORG 2 = 1: 2 in org, but only 1 of those with an org role
            assertEquals(1L, orgsAssignedUsers.get(ORGANISATION_ID_2).longValue());
        }

        @DisplayName("should generate count data with skipped organisation information if any user lookup errors")
        @Test
        void shouldGenerateCountDataWithSkippedOrgInformationIfAnyUserLookupErrors() {

            // GIVEN
            generateAndRegisterOrganisationPolicy(ORGANISATION_ID_1, ROLE_1);
            generateAndRegisterOrganisationPolicy(ORGANISATION_ID_BAD_1, ROLE_2);
            generateAndRegisterOrganisationPolicy(ORGANISATION_ID_BAD_2, ROLE_3);
            generateAndRegisterOrganisationPolicy(ORGANISATION_ID_BAD_3, ROLE_4);

            setUpNocApproverSystemUserTokenCall();
            registerUserLookupForOrg(ORGANISATION_ID_1, List.of(USER_ID_1));
            // register BAD user look ups
            when(prdRepository.findUsersByOrganisation(APPROVER_USER_TOKEN, ORGANISATION_ID_BAD_1)).thenThrow(
                new FeignException.NotFound(
                    "BAD org 1 not found",
                    createRequestForFeignException(),
                    new byte[0],
                    null
                )
            );
            when(prdRepository.findUsersByOrganisation(APPROVER_USER_TOKEN, ORGANISATION_ID_BAD_2)).thenThrow(
                new FeignException.ServiceUnavailable(
                    "BAD org 2 error",
                    createRequestForFeignException(),
                    new byte[0],
                    null
                )
            );
            // unresolvable status
            String unresolvableStatusMessage = "unresolvable status";
            when(prdRepository.findUsersByOrganisation(APPROVER_USER_TOKEN, ORGANISATION_ID_BAD_3)).thenThrow(
                new FeignException.FeignServerException(
                    999,
                    unresolvableStatusMessage,
                    createRequestForFeignException(),
                    new byte[0],
                    null
                )
            );

            when(roleAssignmentService.findRoleAssignmentsByCasesAndUsers(any(), any())).thenReturn(
                List.of(new CaseAssignedUserRole(CASE_ID, USER_ID_1, ROLE_1))
            );

            when(organisationPolicyUtils.findRolesForOrg(policies, ORGANISATION_ID_1)).thenReturn(List.of(ROLE_1));

            // WHEN
            OrganisationsAssignedUsersCountData response =
                classUnderTest.calculateOrganisationsAssignedUsersCountData(CASE_ID);

            // THEN
            assertNotNull(response);
            assertEquals(CASE_ID, response.getCaseId());

            Map<String, Long> orgsAssignedUsers = response.getOrgsAssignedUsers();
            assertEquals(1, orgsAssignedUsers.size()); // i.e. only the working org
            assertEquals(1L, orgsAssignedUsers.get(ORGANISATION_ID_1).longValue());

            Map<String, String> skippedOrgs = response.getSkippedOrgs();
            assertEquals(3, skippedOrgs.size());
            assertThat(skippedOrgs.get(ORGANISATION_ID_BAD_1), containsString("Organisation not found"));
            assertThat(skippedOrgs.get(ORGANISATION_ID_BAD_2), containsString("Error encountered"));
            assertThat(skippedOrgs.get(ORGANISATION_ID_BAD_3), containsString("Error encountered"));
            assertThat(skippedOrgs.get(ORGANISATION_ID_BAD_3), containsString(unresolvableStatusMessage));
        }

        private Request createRequestForFeignException() {
            return Request.create(
                Request.HttpMethod.GET,
                "dummyUrl",
                Map.of(),
                new byte[0],
                Charset.defaultCharset(),
                null
            );
        }

        private void generateAndRegisterOrganisationPolicy(String organisationID, String caseAssignedRole) {

            OrganisationPolicy policy = organisationPolicy(organisationID, caseAssignedRole);

            // register in response
            policies.add(policy);

            // org assigned => true
            when(organisationPolicyUtils.checkIfPolicyHasOrganisationAssigned(policy)).thenReturn(true);
        }

        @SuppressWarnings("SameParameterValue")
        private void generateAndRegisterOrganisationPolicyUnassigned(String caseAssignedRole) {

            OrganisationPolicy policy = OrganisationPolicy.builder()
                .orgPolicyCaseAssignedRole(caseAssignedRole)
                .build();

            // register in response
            policies.add(policy);

            // org assigned => false
            when(organisationPolicyUtils.checkIfPolicyHasOrganisationAssigned(policy)).thenReturn(false);
        }

        private void registerUserLookupForOrg(String organisationID, List<String> userIds) {
            List<ProfessionalUser> users = userIds.stream()
                .map(TestFixtures.ProfessionalUserFixture::user)
                .collect(Collectors.toList());

            when(prdRepository.findUsersByOrganisation(APPROVER_USER_TOKEN, organisationID)).thenReturn(
                usersByOrganisation(organisationID, users)
            );
        }

        private void setUpNocApproverSystemUserTokenCall() {
            when(securityUtils.getNocApproverSystemUserAccessToken()).thenReturn(APPROVER_USER_TOKEN);
        }
    }

    @Nested
    class SaveCountData {

        @Captor
        private ArgumentCaptor<SupplementaryDataUpdateRequest> supplementaryDataUpdateRequestCaptor;

        @DisplayName("should convert count data into SupplementaryData update request and save using system user")
        @Test
        void shouldConvertCountDataIntoSupplementaryDataUpdateRequest_andSaveUsingSystemUser() {

            // GIVEN
            setUpSystemUserTokenCall();

            Map<String, Long> orgsAssignedUsers = new HashMap<>();
            Long expectOrgCount1 = 1L;
            Long expectOrgCount2 = 2L;
            orgsAssignedUsers.put(ORGANISATION_ID_1, expectOrgCount1);
            orgsAssignedUsers.put(ORGANISATION_ID_2, expectOrgCount2);
            OrganisationsAssignedUsersCountData countData = OrganisationsAssignedUsersCountData.builder()
                .caseId(CASE_ID)
                .orgsAssignedUsers(orgsAssignedUsers)
                .build();

            // WHEN
            classUnderTest.saveCountData(countData);

            // THEN
            // ... verify saved by system user
            verify(securityUtils).getCaaSystemUserToken();
            verify(dataStoreApi).updateCaseSupplementaryData(
                eq(SYSTEM_USER_TOKEN), // i.e. using System User
                eq(CASE_ID),
                supplementaryDataUpdateRequestCaptor.capture()
            );

            // ... verify update request looks correct
            SupplementaryDataUpdateRequest updateRequest = supplementaryDataUpdateRequestCaptor.getValue();
            assertNotNull(updateRequest.getSupplementaryDataUpdates());
            assertNotNull(updateRequest.getSupplementaryDataUpdates().getSetMap()); // i.e. has a $set call
            assertNull(updateRequest.getSupplementaryDataUpdates().getFindMap()); // i.e. no $find calls
            assertNull(updateRequest.getSupplementaryDataUpdates().getIncrementalMap()); // i.e. no $inc calls

            // ... verify transformation from CountData to $set data
            Map<String, Object> setMap = updateRequest.getSupplementaryDataUpdates().getSetMap();
            assertEquals(2, setMap.size());
            assertEquals(expectOrgCount1, setMap.get(ORGS_ASSIGNED_USERS_PATH + ORGANISATION_ID_1));
            assertEquals(expectOrgCount2, setMap.get(ORGS_ASSIGNED_USERS_PATH + ORGANISATION_ID_2));
        }

        @DisplayName("should skip zero count records")
        @Test
        void shouldSkipZeroCountRecords() {

            // GIVEN
            setUpSystemUserTokenCall();

            Map<String, Long> orgsAssignedUsers = new HashMap<>();
            orgsAssignedUsers.put(ORGANISATION_ID_1, 1L);
            orgsAssignedUsers.put(ORGANISATION_ID_BAD_1, 0L); // should skip
            orgsAssignedUsers.put(ORGANISATION_ID_2, 2L);
            OrganisationsAssignedUsersCountData countData = OrganisationsAssignedUsersCountData.builder()
                .caseId(CASE_ID)
                .orgsAssignedUsers(orgsAssignedUsers)
                .build();

            // WHEN
            classUnderTest.saveCountData(countData);

            // THEN
            verify(dataStoreApi).updateCaseSupplementaryData(
                eq(SYSTEM_USER_TOKEN), // i.e. using System User
                eq(CASE_ID),
                supplementaryDataUpdateRequestCaptor.capture()
            );
            SupplementaryDataUpdateRequest updateRequest = supplementaryDataUpdateRequestCaptor.getValue();
            Map<String, Object> setMap = updateRequest.getSupplementaryDataUpdates().getSetMap();
            assertEquals(2, setMap.size());
            assertTrue(setMap.containsKey(ORGS_ASSIGNED_USERS_PATH + ORGANISATION_ID_1));
            assertTrue(setMap.containsKey(ORGS_ASSIGNED_USERS_PATH + ORGANISATION_ID_2));
            assertFalse(setMap.containsKey(ORGS_ASSIGNED_USERS_PATH + ORGANISATION_ID_BAD_1)); // skipped zero count
        }

        @DisplayName("should skip save if no data in update request")
        @Test
        void shouldSkipSaveIfNoDataInUpdateRequest() {

            // GIVEN
            Map<String, Long> orgsAssignedUsers = new HashMap<>();
            orgsAssignedUsers.put(ORGANISATION_ID_BAD_1, 0L); // should skip
            OrganisationsAssignedUsersCountData countData = OrganisationsAssignedUsersCountData.builder()
                .caseId(CASE_ID)
                .orgsAssignedUsers(orgsAssignedUsers)
                .build();

            // WHEN
            classUnderTest.saveCountData(countData);

            // THEN
            verify(dataStoreApi, never()).updateCaseSupplementaryData(any(), any(), any()); // i.e. no save made
        }

        private void setUpSystemUserTokenCall() {
            when(securityUtils.getCaaSystemUserToken()).thenReturn(SYSTEM_USER_TOKEN);
        }

    }

}
