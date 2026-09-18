package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@DisplayName("RoleAssignmentResourceTest")
class RoleAssignmentResourceTest {

    private static final String CASE_ID = "111111";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    @DisplayName("shouldSerialiseRoleAssignmentAttributesResourceFields")
    void shouldSerialiseRoleAssignmentAttributesResourceFields() throws JsonProcessingException {
        RoleAssignmentAttributesResource attributes = RoleAssignmentAttributesResource.builder()
            .jurisdiction("DIVORCE")
            .caseType("FinancialRemedy")
            .caseId(CASE_ID)
            .region("Hampshire")
            .location("Southampton")
            .contractType("SALARIED")
            .build();

        String json = OBJECT_MAPPER.writeValueAsString(attributes);
        RoleAssignmentAttributesResource result = OBJECT_MAPPER.readValue(json, RoleAssignmentAttributesResource.class);

        assertThat(result, is(attributes));
    }

    @Test
    @DisplayName("shouldTrackExplicitNullRoleAssignmentAttributesResourceFields")
    void shouldTrackExplicitNullRoleAssignmentAttributesResourceFields() throws JsonProcessingException {
        RoleAssignmentAttributesResource result = OBJECT_MAPPER.readValue(
            "{\"jurisdiction\":null}",
            RoleAssignmentAttributesResource.class
        );

        assertThat(result.isJurisdictionDefined(), is(true));
        assertThat(result.getJurisdiction(), is((String) null));
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
