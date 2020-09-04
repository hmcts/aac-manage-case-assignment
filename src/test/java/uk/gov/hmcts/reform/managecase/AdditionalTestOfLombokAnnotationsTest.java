package uk.gov.hmcts.reform.managecase;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.api.payload.RequestedCaseUnassignment;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignedUsers;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.domain.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional tests of Lombok annotations that are hard to reach.
 */
@SuppressWarnings({"PMD.JUnitTestsShouldIncludeAssert"})
class AdditionalTestOfLombokAnnotationsTest {

    @Nested
    @DisplayName("Test api.payload models")
    class ApiPayloadTests {

        @Test
        @DisplayName("RequestedCaseUnassignment should comply with equals and hashCode methods")
        void requestedCaseUnassignmentTestEqualsContract() {
            EqualsVerifier
                .forClass(RequestedCaseUnassignment.class)
                .suppress(Warning.STRICT_INHERITANCE)
                .verify();
        }

    }

    @Nested
    @DisplayName("Test client.datastore models")
    class ClientDataStoreTests {

        @Test
        @DisplayName("CaseDetails.CaseDetailsBuilder has a toString method")
        void caseDetailsBuilderHasToStringMethod() {
            // ARRANGE
            CaseDetails.CaseDetailsBuilder builder = CaseDetails.builder();

            // ACT + ASSERT
            assertThat(builder.toString()).isNotBlank();
        }

        @Test
        @DisplayName("CaseUserRole.CaseUserRoleBuilder has a toString method")
        void caseUserRoleBuilderHasToStringMethod() {
            // ARRANGE
            CaseUserRole.CaseUserRoleBuilder builder = CaseUserRole.builder();

            // ACT + ASSERT
            assertThat(builder.toString()).isNotBlank();
        }

        @Test
        @DisplayName("CaseUserRole should comply with equals and hashCode methods")
        void caseUserRoleEqualsContract() {
            EqualsVerifier
                .forClass(CaseUserRole.class)
                .suppress(Warning.STRICT_INHERITANCE)
                .verify();
        }

        @Test
        @DisplayName("CaseUserRoleWithOrganisation.CaseUserRoleWithOrganisationBuilder has a toString method")
        void caseUserRoleWithOrganisationBuilderHasToStringMethod() {
            // ARRANGE
            CaseUserRoleWithOrganisation.CaseUserRoleWithOrganisationBuilder builder =
                CaseUserRoleWithOrganisation.withOrganisationBuilder();

            // ACT + ASSERT
            assertThat(builder.toString()).isNotBlank();
        }

        @Test
        @DisplayName("CaseUserRoleWithOrganisation should comply with equals and hashCode methods")
        void caseUserRoleWithOrganisationEqualsContract() {
            EqualsVerifier
                .forClass(CaseUserRoleWithOrganisation.class)
                .suppress(Warning.STRICT_INHERITANCE)
                .verify();
        }
    }

    @Nested
    @DisplayName("Test client.prd models")
    class ClientPrdTests {

        @Test
        @DisplayName("ProfessionalUser.ProfessionalUserBuilder has a toString method")
        void professionalUserBuilderHasToStringMethod() {
            // ARRANGE
            ProfessionalUser.ProfessionalUserBuilder builder = ProfessionalUser.builder();

            // ACT + ASSERT
            assertThat(builder.toString()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Test domain models")
    class DomainTests {

        @Test
        @DisplayName("CaseAssignedUsers.CaseAssignedUsersBuilder has a toString method")
        void caseAssignedUsersBuilderHasToStringMethod() {
            // ARRANGE
            CaseAssignedUsers.CaseAssignedUsersBuilder builder = CaseAssignedUsers.builder();

            // ACT + ASSERT
            assertThat(builder.toString()).isNotBlank();
        }

        @Test
        @DisplayName("OrganisationPolicy.OrganisationPolicyBuilder has a toString method")
        void organisationPolicyBuilderHasToStringMethod() {
            // ARRANGE
            OrganisationPolicy.OrganisationPolicyBuilder builder = OrganisationPolicy.builder();

            // ACT + ASSERT
            assertThat(builder.toString()).isNotBlank();
        }

        @Test
        @DisplayName("UserDetails.UserDetailsBuilder has a toString method")
        void userDetailsBuilderHasToStringMethod() {
            // ARRANGE
            UserDetails.UserDetailsBuilder builder = UserDetails.builder();

            // ACT + ASSERT
            assertThat(builder.toString()).isNotBlank();
        }
    }
}
