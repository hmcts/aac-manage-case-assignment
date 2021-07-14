package uk.gov.hmcts.reform.managecase.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignment;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentAttributes;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignments;
import uk.gov.hmcts.reform.managecase.api.payload.RoleType;
import uk.gov.hmcts.reform.managecase.repository.RoleAssignmentRepository;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentService;
import uk.gov.hmcts.reform.managecase.service.ras.RoleAssignmentsMapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

class RoleAssignmentServiceTest {

    @Mock
    private RoleAssignmentRepository roleAssignmentRepository;

    @Mock
    private RoleAssignmentsMapper roleAssignmentsMapper;

    @Mock
    private RoleAssignmentResponse mockedRoleAssignmentResponse;

    private RoleAssignmentService roleAssignmentService;

    private List<String> caseIds = Arrays.asList("111", "222");
    private List<String> userIds = Arrays.asList("111", "222");
    private static final String CASE_ID = "111111";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        roleAssignmentService = new RoleAssignmentService(roleAssignmentRepository,
                                                          roleAssignmentsMapper);
    }

    @Test
    public void shouldGetRoleAssignmentsByCasesAndUsers() {

        given(roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds))
            .willReturn(mockedRoleAssignmentResponse);

        given(roleAssignmentsMapper.toRoleAssignments(mockedRoleAssignmentResponse))
            .willReturn(getRoleAssignments());

        final List<CaseAssignedUserRole> caseAssignedUserRole =
            roleAssignmentService.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

        assertEquals(2, caseAssignedUserRole.size());
        assertThat(caseAssignedUserRole.get(0).getCaseDataId(), is(CASE_ID));
    }

    private RoleAssignments getRoleAssignments() {

        final Instant currentTIme = Instant.now();
        final long oneHour = 3600000;

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
