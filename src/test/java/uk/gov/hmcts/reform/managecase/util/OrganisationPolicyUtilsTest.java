package uk.gov.hmcts.reform.managecase.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ORGANIZATION_ID;

@ExtendWith(MockitoExtension.class)
class OrganisationPolicyUtilsTest {

    @Mock
    private JacksonUtils jacksonUtils;

    @InjectMocks
    OrganisationPolicyUtils classUnderTest;

    @DisplayName("findPolicies: should extract org policies from case details and convert")
    @Test
    void findPolicies_shouldExtractOrgPoliciesFromCaseDetails_andConvert() {

        // GIVEN
        CaseDetails caseDetails = Mockito.mock(CaseDetails.class);

        JsonNode policyNode1 = Mockito.mock(JsonNode.class);
        JsonNode policyNode2 = Mockito.mock(JsonNode.class);
        JsonNode policyNode3 = Mockito.mock(JsonNode.class);

        // mock find
        when(caseDetails.findOrganisationPolicyNodes()).thenReturn(
            List.of(policyNode1, policyNode2, policyNode3)
        );

        OrganisationPolicy policy1 = OrganisationPolicy.builder().build();
        OrganisationPolicy policy2 = OrganisationPolicy.builder().build();
        OrganisationPolicy policy3 = OrganisationPolicy.builder().build();

        // mock convert
        when(jacksonUtils.convertValue(policyNode1, OrganisationPolicy.class)).thenReturn(policy1);
        when(jacksonUtils.convertValue(policyNode2, OrganisationPolicy.class)).thenReturn(policy2);
        when(jacksonUtils.convertValue(policyNode3, OrganisationPolicy.class)).thenReturn(policy3);

        // WHEN
        List<OrganisationPolicy> response = classUnderTest.findPolicies(caseDetails);

        // THEN
        assertEquals(3, response.size());
        assertThat(response, containsInAnyOrder(policy1, policy2, policy3));

        verify(caseDetails).findOrganisationPolicyNodes();
        verify(jacksonUtils, times(3)).convertValue(any(JsonNode.class), eq(OrganisationPolicy.class));

    }

    @DisplayName(
        "findRolesForOrg: should find roles for Organisation but ignore when null, null orgId and non-matching orgId"
    )
    @Test
    void findRolesForOrg_shouldFindRolesForOrg_butIgnoreNullOrganisationAndNullOrgIdAndNonMatchingOrgId() {

        // GIVEN
        OrganisationPolicy policyNullOrg = OrganisationPolicy.builder()
            .orgPolicyCaseAssignedRole("IGNORE-ME-Ref 1")
            .organisation(null)
            .build();

        OrganisationPolicy policyNullOrgId = OrganisationPolicy.builder()
            .orgPolicyCaseAssignedRole("IGNORE-ME-Ref 2")
            .organisation(Organisation.builder()
                              .organisationID(null)
                              .build())
            .build();

        String findMeRef1 = "FIND-ME-Ref 1";
        OrganisationPolicy policyOrgOk1 = OrganisationPolicy.builder()
            .orgPolicyCaseAssignedRole(findMeRef1)
            .organisation(Organisation.builder()
                              .organisationID(ORGANIZATION_ID) // NB: matching OrgId
                              .build())
            .build();

        OrganisationPolicy policyOrgAnother = OrganisationPolicy.builder()
            .orgPolicyCaseAssignedRole("IGNORE-ME-Ref 4")
            .organisation(Organisation.builder()
                              .organisationID("AnotherId")
                              .build())
            .build();

        String findMeRef2 = "FIND-ME-Ref 2";
        OrganisationPolicy policyOrgOk2 = OrganisationPolicy.builder()
            .orgPolicyCaseAssignedRole(findMeRef2)
            .organisation(Organisation.builder()
                              .organisationID(ORGANIZATION_ID) // NB: matching OrgId
                              .build())
            .build();

        // WHEN
        List<String> response = classUnderTest.findRolesForOrg(
            List.of(
                policyNullOrg,
                policyNullOrgId,
                policyOrgOk1,
                policyOrgAnother,
                policyOrgOk2
            ),
            ORGANIZATION_ID
        );

        // THEN
        // .. should have found the two FIND-ME policy references
        assertEquals(2, response.size());
        assertThat(response, containsInAnyOrder(findMeRef1, findMeRef2));
    }

    @ParameterizedTest(name = "checkIfPolicyHasOrganisationAssigned: should return FALSE if unassigned to org: {0}")
    @MethodSource("testOrgPoliciesWithoutOrganisationAssigned")
    @NullSource
    void checkIfPolicyHasOrganisationAssigned_shouldReturnFalseIfUnassignedToOrg(OrganisationPolicy policy) {

        assertFalse(classUnderTest.checkIfPolicyHasOrganisationAssigned(policy));
    }

    @DisplayName("checkIfPolicyHasOrganisationAssigned: should return TRUE if assigned to org")
    @Test
    void checkIfPolicyHasOrganisationAssigned_shouldReturnTrueIfAssignedToOrg() {

        // GIVEN
        OrganisationPolicy policy = OrganisationPolicy.builder()
            .orgPolicyCaseAssignedRole("Organisation with ID")
            .organisation(Organisation.builder()
                              .organisationID(ORGANIZATION_ID)
                              .build())
            .build();

        // WHEN / THEN
        assertTrue(classUnderTest.checkIfPolicyHasOrganisationAssigned(policy));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> testOrgPoliciesWithoutOrganisationAssigned() {
        return Stream.of(
            // NB: params correspond to:
            // * policy :: policy to test
            Arguments.of(OrganisationPolicy.builder()
                             .orgPolicyCaseAssignedRole("Null Organisation")
                             .organisation(null)
                             .build()),
            Arguments.of(OrganisationPolicy.builder()
                             .orgPolicyCaseAssignedRole("Organisation has null ID")
                             .organisation(Organisation.builder()
                                               .organisationID(null)
                                               .build())
                             .build())
        );
    }
}
