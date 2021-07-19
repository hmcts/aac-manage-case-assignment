package uk.gov.hmcts.reform.managecase.api.controller;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.managecase.api.errorhandling.BadRequestException;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRole;
import uk.gov.hmcts.reform.managecase.api.payload.CaseAssignedUserRolesResource;
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
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.EMPTY_CASE_ID_LIST;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.USER_ID_INVALID;

class CaseAssignedUserRolesControllerTest {

    @Mock
    private UIDService caseReferenceService;

    @Mock
    private CaseAssignedUserRolesOperation caseAssignedUserRolesOperation;

    private CaseAssignedUserRolesController controller;

    private static final String CASE_ID_GOOD = "4444333322221111";
    private static final String CASE_ID_BAD = "1234";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(caseReferenceService.validateUID(CASE_ID_GOOD)).thenReturn(true);
        when(caseReferenceService.validateUID(CASE_ID_BAD)).thenReturn(false);

        controller = new CaseAssignedUserRolesController(caseAssignedUserRolesOperation, caseReferenceService);
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
                BadRequestException.class,() -> controller.getCaseUserRoles(caseIds, optionalUserIds));

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
}
