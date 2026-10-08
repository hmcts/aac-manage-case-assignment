package uk.gov.hmcts.reform.managecase.api.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("RoleAssignmentResourceTest")
class RoleAssignmentResourceTest {

    private static final String CASE_ID = "111111";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    @DisplayName("shouldPassForIsAnExpiredRoleAssignment")
    void shouldPassForIsAnExpiredRoleAssignment() {

        final long oneHour = 3600000;
        final RoleAssignments roleAssignments = getRoleAssignments(oneHour);
        assertThat(roleAssignments.getRoleAssignmentsList().getFirst().isNotExpiredRoleAssignment()).isTrue();
        assertThat(roleAssignments.getRoleAssignmentsList().get(1).isNotExpiredRoleAssignment()).isTrue();
    }

    @Test
    @DisplayName("shouldNotPassForIsAnExpiredRoleAssignment")
    void shouldNotPassForIsAnExpiredRoleAssignment() {

        final long oneHour = 0;
        final RoleAssignments roleAssignments = getRoleAssignments(oneHour);
        assertThat(roleAssignments.getRoleAssignmentsList().getFirst().isNotExpiredRoleAssignment()).isFalse();
        assertThat(roleAssignments.getRoleAssignmentsList().get(1).isNotExpiredRoleAssignment()).isFalse();
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

        assertThat(result).isEqualTo(attributes);
    }

    @Test
    @DisplayName("shouldTrackExplicitNullRoleAssignmentAttributesResourceFields")
    void shouldTrackExplicitNullRoleAssignmentAttributesResourceFields() throws JsonProcessingException {
        RoleAssignmentAttributesResource result = OBJECT_MAPPER.readValue(
            """
                {
                    "jurisdiction":null,
                    "caseType":null,
                    "caseId":null,
                    "region":null,
                    "location":null,
                    "contractType":null
                }
                """,
            RoleAssignmentAttributesResource.class
        );

        assertAll(
            () -> assertThat(result.getJurisdiction()).isNull(),
            () -> assertThat(result.getCaseType()).isNull(),
            () -> assertThat(result.getCaseId()).isNull(),
            () -> assertThat(result.getRegion()).isNull(),
            () -> assertThat(result.getLocation()).isNull(),
            () -> assertThat(result.getContractType()).isNull(),
            () -> assertThat(result.isJurisdictionDefined()).isTrue(),
            () -> assertThat(result.isCaseTypeDefined()).isTrue(),
            () -> assertThat(result.isCaseIdDefined()).isTrue(),
            () -> assertThat(result.isRegionDefined()).isTrue(),
            () -> assertThat(result.isLocationDefined()).isTrue(),
            () -> assertThat(result.isContractTypeDefined()).isTrue()
        );
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
