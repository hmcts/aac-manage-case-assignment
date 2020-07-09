package uk.gov.hmcts.reform.managecase.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignment;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import javax.validation.ValidationException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.caseDetails;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseDetailsFixture.organisationPolicy;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.user;
import static uk.gov.hmcts.reform.managecase.TestFixtures.ProfessionalUserFixture.usersByOrganisation;
import static uk.gov.hmcts.reform.managecase.service.CaseAssignmentService.ASSIGNEE_ORGA_ERROR;
import static uk.gov.hmcts.reform.managecase.service.CaseAssignmentService.ASSIGNEE_ROLE_ERROR;
import static uk.gov.hmcts.reform.managecase.service.CaseAssignmentService.CASE_NOT_FOUND;

@SuppressWarnings({"PMD.MethodNamingConventions"})
class CaseAssignmentServiceTest {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String ASSIGNEE_ID = "ae2eb34c-816a-4eea-b714-6654d022fcef";
    private static final String CASE_ID = "12345678";
    private static final String ANOTHER_USER = "vcd345cvs-816a-4eea-b714-6654d022fcef";
    private static final String ORG_POLICY_ROLE = "caseworker-probate";
    private static final String ORGANIZATION_ID = "TEST_ORG";

    @InjectMocks
    private CaseAssignmentService service;

    @Mock
    private DataStoreRepository dataStoreRepository;
    @Mock
    private PrdRepository prdRepository;
    @Mock
    private JacksonUtils jacksonUtils;

    private CaseAssignment caseAssignment;

    @BeforeEach
    void setUp() {
        initMocks(this);

        caseAssignment = new CaseAssignment(CASE_TYPE_ID, CASE_ID, ASSIGNEE_ID);

        given(dataStoreRepository.findCaseBy(CASE_TYPE_ID, CASE_ID))
                .willReturn(Optional.of(caseDetails(ORGANIZATION_ID, ORG_POLICY_ROLE)));
        given(prdRepository.findUsersByOrganisation())
                .willReturn(usersByOrganisation(user(ASSIGNEE_ID, "caseworker-AUTOTEST1-solicitor")));
    }

    @Test
    @DisplayName("should assign case in the organisation")
    void shouldAssignCaseAccess() {

        given(jacksonUtils.convertValue(any(JsonNode.class), eq(OrganisationPolicy.class)))
                .willReturn(organisationPolicy(ORGANIZATION_ID, ORG_POLICY_ROLE));

        String role = service.assignCaseAccess(caseAssignment);

        assertThat(role).isEqualTo(ORG_POLICY_ROLE);

        verify(dataStoreRepository).assignCase(CASE_ID, ORG_POLICY_ROLE, ASSIGNEE_ID);
    }

    @Test
    @DisplayName("should throw validation error when case is not found")
    void shouldThrowValidationException_whenCaseNotFound() {

        given(dataStoreRepository.findCaseBy(CASE_TYPE_ID, CASE_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignCaseAccess(caseAssignment))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(CASE_NOT_FOUND);
    }

    @Test
    @DisplayName("should throw validation error when assignee is not found in the organisation")
    void shouldThrowValidationException_whenAssigneeNotExists() {

        given(prdRepository.findUsersByOrganisation())
                .willReturn(usersByOrganisation(user(ANOTHER_USER)));

        assertThatThrownBy(() -> service.assignCaseAccess(caseAssignment))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(ASSIGNEE_ORGA_ERROR);
    }

    @Test
    @DisplayName("should throw validation error when assignee doesn't have jurisdiction solicitor role")
    void shouldThrowValidationException_whenAssigneeRolesNotMatching() {

        given(prdRepository.findUsersByOrganisation())
                .willReturn(usersByOrganisation(user(ASSIGNEE_ID, "caseworker-AUTOTEST2-solicitor")));

        assertThatThrownBy(() -> service.assignCaseAccess(caseAssignment))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(ASSIGNEE_ROLE_ERROR);
    }
}
