package uk.gov.hmcts.reform.managecase.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignment;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentAttributesResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignments;
import uk.gov.hmcts.reform.managecase.domain.GrantType;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentsMapper;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentsMapperImpl;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("RoleAssignmentMapperTest")
public class RoleAssignmentsMapperTest {
    public static final String CASE_ID1 = "caseId1";
    public static final String CASE_ID2 = "caseId2";
    public static final String ASSIGNMENT_1 = "assignment1";
    public static final String ASSIGNMENT_2 = "assignment2";
    private static final Instant BEGIN_TIME = Instant.parse("2015-10-21T13:32:21.123Z");
    private static final Instant END_TIME = Instant.parse("2215-11-04T14:43:22.456Z");
    private static final Instant CREATED = Instant.parse("2020-12-04T15:54:23.789Z");

    private final RoleAssignmentsMapper instance = new RoleAssignmentsMapperImpl();

    @Nested
    class ToRoleAssignments {

        @Test
        public void shouldMapToRoleAssignments() {
            RoleAssignmentResource roleAssignment1 = createRoleAssignmentRecord(ASSIGNMENT_1, CASE_ID1);
            RoleAssignmentResource roleAssignment2 = createRoleAssignmentRecord(ASSIGNMENT_2, CASE_ID2);
            RoleAssignmentResponse response = createRoleAssignmentResponse(asList(
                roleAssignment1, roleAssignment2
            ));

            RoleAssignments mapped = instance.toRoleAssignments(response);

            List<RoleAssignment> roleAssignments = mapped.getRoleAssignmentsList();
            RoleAssignment firstRoleAssignment = roleAssignments.getFirst();
            RoleAssignment secondRoleAssignment = roleAssignments.get(1);

            assertAll(
                () -> assertThat(roleAssignments.size(), is(2)),

                () -> assertThat(firstRoleAssignment.getId(), is(ASSIGNMENT_1)),
                () -> assertThat(firstRoleAssignment.getActorIdType(), is(roleAssignment1.getActorIdType())),
                () -> assertThat(firstRoleAssignment.getActorId(), is(roleAssignment1.getActorId())),
                () -> assertThat(firstRoleAssignment.getRoleType(), is(roleAssignment1.getRoleType())),
                () -> assertThat(firstRoleAssignment.getRoleName(), is(roleAssignment1.getRoleName())),
                () -> assertThat(firstRoleAssignment.getClassification(),
                                 is(roleAssignment1.getClassification())),
                () -> assertThat(firstRoleAssignment.getGrantType(), is(roleAssignment1.getGrantType())),
                () -> assertThat(firstRoleAssignment.getRoleCategory(), is(roleAssignment1.getRoleCategory())),
                () -> assertThat(firstRoleAssignment.getReadOnly(), is(roleAssignment1.getReadOnly())),
                () -> assertThat(firstRoleAssignment.getBeginTime(), is(roleAssignment1.getBeginTime())),
                () -> assertThat(firstRoleAssignment.getEndTime(), is(roleAssignment1.getEndTime())),
                () -> assertThat(firstRoleAssignment.getCreated(), is(roleAssignment1.getCreated())),
                () -> assertThat(firstRoleAssignment.getAuthorisations().size(), is(0)),

                () -> assertThat(firstRoleAssignment.getAttributes().getJurisdiction(),
                                 is(Optional.ofNullable(roleAssignment1.getAttributes().getJurisdiction()))),
                () -> assertThat(firstRoleAssignment.getAttributes().getCaseId(),
                                 is(Optional.ofNullable(roleAssignment1.getAttributes().getCaseId()))),
                () -> assertThat(firstRoleAssignment.getAttributes().getRegion(),
                                 is(Optional.ofNullable(roleAssignment1.getAttributes().getRegion()))),
                () -> assertThat(firstRoleAssignment.getAttributes().getLocation(),
                                 is(Optional.ofNullable(roleAssignment1.getAttributes().getLocation()))),
                () -> assertThat(firstRoleAssignment.getAttributes().getContractType(),
                                 is(Optional.ofNullable(roleAssignment1.getAttributes().getContractType()))),

                () -> assertThat(secondRoleAssignment.getId(), is(ASSIGNMENT_2)),
                () -> assertThat(secondRoleAssignment.getAttributes().getCaseId(),
                                 is(Optional.ofNullable(roleAssignment2.getAttributes().getCaseId()))),
                () -> assertThat(secondRoleAssignment.getAttributes().getJurisdiction(),
                                 is(Optional.ofNullable(roleAssignment2.getAttributes().getJurisdiction()))),
                () -> assertThat(secondRoleAssignment.getAttributes().getCaseType(),
                                 is(Optional.ofNullable(roleAssignment2.getAttributes().getCaseType()))),
                () -> assertThat(secondRoleAssignment.getAttributes().getContractType(),
                                 is(Optional.ofNullable(roleAssignment2.getAttributes().getContractType())))
            );
        }

        @Test
        public void shouldMapNullRoleAssignmentResponse() {
            assertNull(instance.toRoleAssignments(null));
        }

        @Test
        public void shouldMapNullRoleAssignmentResourceList() {
            RoleAssignmentResponse response = createRoleAssignmentResponse(null);

            RoleAssignments mapped = instance.toRoleAssignments(response);

            List<RoleAssignment> roleAssignments = mapped.getRoleAssignmentsList();
            assertEquals(0, roleAssignments.size());
        }

        @Test
        public void shouldMapNullRoleAssignmentResource() {
            RoleAssignmentResource roleAssignment = null;
            RoleAssignmentResponse response = createRoleAssignmentResponse(singletonList(roleAssignment));

            RoleAssignments mapped = instance.toRoleAssignments(response);

            List<RoleAssignment> roleAssignments = mapped.getRoleAssignmentsList();
            assertAll(
                () -> assertThat(roleAssignments.size(), is(1)),
                () -> assertNull(roleAssignments.getFirst())
            );
        }

        @Test
        public void shouldMapNullRoleAssignmentAttributes() {
            RoleAssignmentResource roleAssignment = RoleAssignmentResource.builder()
                .id(ASSIGNMENT_1)
                .attributes(null)
                .build();
            RoleAssignmentResponse response = createRoleAssignmentResponse(singletonList(roleAssignment));

            RoleAssignments mapped = instance.toRoleAssignments(response);

            List<RoleAssignment> roleAssignments = mapped.getRoleAssignmentsList();
            assertAll(
                () -> assertThat(roleAssignments.size(), is(1)),
                () -> assertThat(roleAssignments.getFirst().getId(), is(ASSIGNMENT_1)),
                () -> assertNull(roleAssignments.getFirst().getAttributes())
            );
        }
    }

    private static RoleAssignmentResponse createRoleAssignmentResponse(
        List<RoleAssignmentResource> roleAssignments) {
        return RoleAssignmentResponse.builder()
            .roleAssignments(roleAssignments)
            .build();
    }

    private static RoleAssignmentResource createRoleAssignmentRecord(String id, String caseId) {
        return RoleAssignmentResource.builder()
            .id(id)
            .actorIdType("IDAM") // currently IDAM
            .actorId("aecfec12-1f9a-40cb-bd8c-7a9f3506e67c")
            .roleType("CASE") // ORGANISATION, CASE
            .roleName("judiciary")
            .classification("PUBLIC")
            .grantType(GrantType.STANDARD.name()) // BASIC, STANDARD, SPECIFIC, CHALLENGED, EXCLUDED
            .roleCategory("JUDICIAL") // JUDICIAL, STAFF
            .readOnly(false)
            .beginTime(BEGIN_TIME)
            .endTime(END_TIME)
            .created(CREATED)
            .authorisations(Collections.emptyList())
            .attributes(createRoleAssignmentRecordAttribute(caseId))
            .build();
    }

    private static RoleAssignmentAttributesResource createRoleAssignmentRecordAttribute(String caseId) {
        return RoleAssignmentAttributesResource.builder()
            .jurisdiction("DIVORCE")
            .caseId(caseId)
            .caseType("FT_Tabs")
            .region("Hampshire")
            .location("Southampton")
            .contractType("SALARIED") // SALARIED, FEEPAY
            .build();
    }
}
