package uk.gov.hmcts.reform.managecase.repository;


import feign.FeignException;
import feign.Request;
import static feign.Request.Body.empty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseCouldNotBeFetchedException;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRolesRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;

import java.util.List;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.managecase.service.CaseAssignmentService.CASE_COULD_NOT_BE_FETCHED;


@SuppressWarnings({"PMD.ExcessiveImports", "PMD.TooManyMethods"})
class DataStoreRepositoryTest {

    private static final String ASSIGNEE_ID = "0a5874a4-3f38-4bbd-ba4c";
    private static final String ROLE = "caseworker-probate";
    private static final String CASE_ID = "12345678";
    private static final String CASE_ID2 = "87654321";
    private static final String ORG_ID = "organisation1";

    @Mock
    private DataStoreApiClient dataStoreApi;

    @InjectMocks
    private DefaultDataStoreRepository repository;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    @DisplayName("find case by id using external facing API")
    void shouldFindCaseByIdUsingExternalApi() {
        // ARRANGE
        CaseDetails caseDetails = CaseDetails.builder().build();
        given(dataStoreApi.getCaseDetailsByCaseIdViaExternalApi(CASE_ID)).willReturn(caseDetails);

        // ACT
        CaseDetails result = repository.findCaseByCaseIdExternalApi(CASE_ID);

        // ASSERT
        assertThat(result).isEqualTo(caseDetails);
        verify(dataStoreApi).getCaseDetailsByCaseIdViaExternalApi(eq(CASE_ID));
    }

    @Test
    @DisplayName("find case by id using external facing API return no cases")
    void shouldReturnNoCaseForFindCaseByIdUsingExternalApi() {
        // ARRANGE
        given(dataStoreApi.getCaseDetailsByCaseIdViaExternalApi(CASE_ID)).willReturn(null);

        // ACT
        CaseDetails result = repository.findCaseByCaseIdExternalApi(CASE_ID);

        // ASSERT
        assertThat(result).isNull();
        verify(dataStoreApi).getCaseDetailsByCaseIdViaExternalApi(eq(CASE_ID));
    }

    @Test
    @DisplayName("find case by id using external facing API throws exception if no case found")
    void shouldThrowExceptionForFindCaseByIdUsingExternalApi() {
        Request request = Request.create(Request.HttpMethod.POST, "url", emptyMap(), empty(), null);

        // ARRANGE
        given(dataStoreApi.getCaseDetailsByCaseIdViaExternalApi(CASE_ID))
            .willThrow(new FeignException.NotFound("Not found", request, null));

        // ACT &  ASSERT
        assertThatThrownBy(() -> repository.findCaseByCaseIdExternalApi(CASE_ID))
            .isInstanceOf(CaseCouldNotBeFetchedException.class)
            .hasMessageContaining(CASE_COULD_NOT_BE_FETCHED);
    }

    @Test
    @DisplayName("Assign case access")
    void shouldAssignCase() {
        // ARRANGE
        doNothing().when(dataStoreApi).assignCase(any(CaseUserRolesRequest.class));

        // ACT
        repository.assignCase(List.of(ROLE), CASE_ID, ASSIGNEE_ID, ORG_ID);

        // ASSERT
        ArgumentCaptor<CaseUserRolesRequest> captor = ArgumentCaptor.forClass(CaseUserRolesRequest.class);
        verify(dataStoreApi).assignCase(captor.capture());
        List<CaseUserRoleWithOrganisation> caseUserRoles = captor.getValue().getCaseUsers();

        assertThat(caseUserRoles.size()).isEqualTo(1);
        CaseUserRoleWithOrganisation caseUserRole = caseUserRoles.get(0);

        assertThat(caseUserRole.getCaseId()).isEqualTo(CASE_ID);
        assertThat(caseUserRole.getCaseRole()).isEqualTo(ROLE);
        assertThat(caseUserRole.getUserId()).isEqualTo(ASSIGNEE_ID);
        assertThat(caseUserRole.getOrganisationId()).isEqualTo(ORG_ID);
    }

    @Test
    @DisplayName("Get case assignments")
    void shouldGetCaseAssignments() {
        // ARRANGE
        List<String> caseIds = List.of(CASE_ID);
        List<String> userIds = List.of(ASSIGNEE_ID);

        CaseUserRole inputRole = CaseUserRole.builder()
                .caseId(CASE_ID)
                .userId(ASSIGNEE_ID)
                .caseRole(ROLE)
                .build();

        given(dataStoreApi.getCaseAssignments(caseIds, userIds))
                .willReturn(new CaseUserRoleResource(List.of(inputRole)));

        // ACT
        List<CaseUserRole> caseUserRoles = repository.getCaseAssignments(caseIds, userIds);

        // ASSERT
        assertThat(caseUserRoles.size()).isEqualTo(1);
        CaseUserRole caseUserRole = caseUserRoles.get(0);

        assertThat(caseUserRole.getCaseId()).isEqualTo(CASE_ID);
        assertThat(caseUserRole.getCaseRole()).isEqualTo(ROLE);
        assertThat(caseUserRole.getUserId()).isEqualTo(ASSIGNEE_ID);
    }

    @Test
    @DisplayName("Remove case user roles")
    void shouldRemoveCaseUserRoles() {
        // ARRANGE
        doNothing().when(dataStoreApi).removeCaseUserRoles(any(CaseUserRolesRequest.class));

        List<CaseUserRole> caseUserRoles = List.of(
            new CaseUserRole(CASE_ID, ASSIGNEE_ID, ROLE),
            new CaseUserRole(CASE_ID2, ASSIGNEE_ID, ROLE)
        );

        // ACT
        repository.removeCaseUserRoles(caseUserRoles, ORG_ID);

        // ASSERT
        ArgumentCaptor<CaseUserRolesRequest> captor = ArgumentCaptor.forClass(CaseUserRolesRequest.class);
        verify(dataStoreApi).removeCaseUserRoles(captor.capture());
        List<CaseUserRoleWithOrganisation> caseUserRolesWithOrganisation = captor.getValue().getCaseUsers();

        assertThat(caseUserRolesWithOrganisation)
            .hasSameSizeAs(caseUserRoles)
            .containsAll(List.of(
                new CaseUserRoleWithOrganisation(CASE_ID, ASSIGNEE_ID, ROLE, ORG_ID),
                new CaseUserRoleWithOrganisation(CASE_ID2, ASSIGNEE_ID, ROLE, ORG_ID)
            ));
    }
}
