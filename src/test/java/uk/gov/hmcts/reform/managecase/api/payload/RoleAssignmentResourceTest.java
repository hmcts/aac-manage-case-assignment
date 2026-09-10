package uk.gov.hmcts.reform.managecase.api.payload;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.STANDARD;
import static uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType.SPECIFIC;

@DisplayName("RoleAssignmentResourceTest")
class RoleAssignmentResourceTest {

    private static final String CASE_ID = "111111";

    @Test
    @DisplayName("shouldPassForIsAnExpiredRoleAssignment")
    void shouldPassForIsAnExpiredRoleAssignment() {

        final long oneHour = 3600000;
        final RoleAssignments roleAssignments = getRoleAssignments(oneHour);
        roleAssignments.getRoleAssignmentsList().get(0).isNotExpiredRoleAssignment();
        assertThat(roleAssignments.getRoleAssignmentsList().get(0).isNotExpiredRoleAssignment(), is(true));
        assertThat(roleAssignments.getRoleAssignmentsList().get(1).isNotExpiredRoleAssignment(), is(true));
    }

    @Test
    @DisplayName("shouldNotPassForIsAnExpiredRoleAssignment")
    void shouldNotPassForIsAnExpiredRoleAssignment() {

        final long oneHour = 0;
        final RoleAssignments roleAssignments = getRoleAssignments(oneHour);
        roleAssignments.getRoleAssignmentsList().get(0).isNotExpiredRoleAssignment();
        assertThat(roleAssignments.getRoleAssignmentsList().get(0).isNotExpiredRoleAssignment(), is(false));
        assertThat(roleAssignments.getRoleAssignmentsList().get(1).isNotExpiredRoleAssignment(), is(false));
    }

    @Test
    @DisplayName("shouldPassForIsGrantType")
    void shouldPassForIsGrantType() {
        RoleAssignment roleAssignment = RoleAssignment.builder().grantType(SPECIFIC.name()).build();

        assertThat(roleAssignment.isGrantType(SPECIFIC), is(true));
    }

    @Test
    @DisplayName("shouldNotPassForIsGrantType")
    void shouldNotPassForIsGrantType() {
        RoleAssignment roleAssignment = RoleAssignment.builder().grantType(null).build();

        assertThat(roleAssignment.isGrantType(SPECIFIC), is(false));
    }

    @Test
    @DisplayName("shouldNotPassForIsGrantTypeWhenGrantTypeDoesNotMatch")
    void shouldNotPassForIsGrantTypeWhenGrantTypeDoesNotMatch() {
        RoleAssignment roleAssignment = RoleAssignment.builder().grantType(STANDARD.name()).build();

        assertThat(roleAssignment.isGrantType(SPECIFIC), is(false));
    }

    @Test
    @DisplayName("shouldThrowWhenRequestedGrantTypeIsNullAndRoleGrantTypeIsSet")
    void shouldThrowWhenRequestedGrantTypeIsNullAndRoleGrantTypeIsSet() {
        RoleAssignment roleAssignment = RoleAssignment.builder().grantType(SPECIFIC.name()).build();

        assertThrows(NullPointerException.class, () -> roleAssignment.isGrantType(null));
    }


    private RoleAssignments getRoleAssignments(final long oneHour) {

        final Instant currentTIme = Instant.now();
        final RoleAssignmentAttributes roleAssignmentAttributes =
            RoleAssignmentAttributes.builder().caseId(Optional.of(CASE_ID)).build();

        final List<RoleAssignment> roleAssignments = Arrays.asList(

            RoleAssignment.builder().actorId("actorId").roleType(RoleType.CASE.name())
                .attributes(roleAssignmentAttributes)
                .beginTime(currentTIme.minusMillis(oneHour)).endTime(currentTIme.plusMillis(oneHour)).build(),

            RoleAssignment.builder().actorId("actorId1").roleType(RoleType.CASE.name())
                .attributes(roleAssignmentAttributes)
                .beginTime(currentTIme.minusMillis(oneHour)).endTime(currentTIme.plusMillis(oneHour)).build()
        );
        return RoleAssignments.builder().roleAssignmentsList(roleAssignments).build();
    }
}
