package uk.gov.hmcts.reform.managecase.repository;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseSearchResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRolesRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewActionableEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewJurisdiction;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewType;
import uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.HeaderGroupMetadata;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewHeader;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewHeaderGroup;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewItem;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST;
import static uk.gov.hmcts.reform.managecase.repository.DefaultDataStoreRepository.ES_QUERY;

@SuppressWarnings({"PMD.ExcessiveImports"})
class DataStoreRepositoryTest {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String ASSIGNEE_ID = "0a5874a4-3f38-4bbd-ba4c";
    private static final String ROLE = "caseworker-probate";
    private static final String CASE_ID = "12345678";
    private static final String CASE_ID2 = "87654321";
    private static final String ORG_ID = "organisation1";
    private static final String JURISDICTION = "Jurisdiction";

    @Mock
    private DataStoreApiClient dataStoreApi;

    @InjectMocks
    private DefaultDataStoreRepository repository;


    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    @DisplayName("Find case by caseTypeId and caseId")
    void shouldFindCaseBy() {
        // ARRANGE
        CaseDetails caseDetails = CaseDetails.builder()
                .caseTypeId(CASE_TYPE_ID)
                .reference(CASE_ID)
                .build();
        CaseSearchResponse response = new CaseSearchResponse(Lists.newArrayList(caseDetails));
        given(dataStoreApi.searchCases(anyString(), anyString())).willReturn(response);

        // ACT
        Optional<CaseDetails> result = repository.findCaseBy(CASE_TYPE_ID, CASE_ID);

        // ASSERT
        assertThat(result).get().isEqualTo(caseDetails);

        verify(dataStoreApi).searchCases(eq(CASE_TYPE_ID), eq(String.format(ES_QUERY, CASE_ID)));
    }

    @Test
    @DisplayName("Find case return no cases")
    void shouldReturnNoCaseForSearch() {
        // ARRANGE
        CaseSearchResponse response = new CaseSearchResponse(Lists.newArrayList());
        given(dataStoreApi.searchCases(anyString(), anyString())).willReturn(response);

        // ACT
        Optional<CaseDetails> result = repository.findCaseBy(CASE_TYPE_ID, CASE_ID);

        // ASSERT
        assertThat(result).isNotPresent();
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


    @Test
    @DisplayName("Find case by caseTypeId and caseId")
    void shouldFindCaseByExternalES() {
        // ARRANGE
        SearchResultViewHeader searchResultViewHeader = new SearchResultViewHeader();
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setType(PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST);
        searchResultViewHeader.setCaseFieldTypeDefinition(fieldTypeDefinition);
        SearchResultViewHeaderGroup searchResultViewHeaderGroup = new SearchResultViewHeaderGroup(
            new HeaderGroupMetadata(JURISDICTION, CASE_TYPE_ID),
            Arrays.asList(searchResultViewHeader), Arrays.asList("111", "222")
        );
        SearchResultViewItem searchResultViewItem = new SearchResultViewItem();
        CaseSearchResultViewResource caseSearchResultViewResource = new CaseSearchResultViewResource();
        caseSearchResultViewResource.setCases(Arrays.asList(searchResultViewItem));
        caseSearchResultViewResource.setHeaders(Arrays.asList(searchResultViewHeaderGroup));
        given(dataStoreApi.internalSearchCases(anyString(), any(), anyString()))
            .willReturn(caseSearchResultViewResource);

        // ACT
        CaseSearchResultViewResource result = repository.findCaseBy(CASE_TYPE_ID, null, CASE_ID);

        // ASSERT
        assertThat(result).isEqualTo(caseSearchResultViewResource);
    }

    @Test
    @DisplayName("Find case by case and caseId")
    void shouldFindCaseByCaseId() {
        // ARRANGE

        CaseViewActionableEvent caseViewActionableEvent = new CaseViewActionableEvent();
        CaseViewResource caseViewResource = new CaseViewResource();
        caseViewResource.setCaseViewActionableEvents(new CaseViewActionableEvent[] {caseViewActionableEvent});
        CaseViewType caseViewType = new CaseViewType();
        caseViewType.setName(CASE_TYPE_ID);
        caseViewType.setId(CASE_TYPE_ID);
        CaseViewJurisdiction caseViewJurisdiction = new CaseViewJurisdiction();
        caseViewJurisdiction.setId(JURISDICTION);
        caseViewType.setJurisdiction(caseViewJurisdiction);
        caseViewResource.setCaseType(caseViewType);
        given(dataStoreApi.getCaseDetailsByCaseId(anyString())).willReturn(caseViewResource);

        // ACT
        CaseViewResource result = repository.findCaseByCaseId(CASE_ID);

        // ASSERT
        assertThat(result).isEqualTo(caseViewResource);
    }

}
