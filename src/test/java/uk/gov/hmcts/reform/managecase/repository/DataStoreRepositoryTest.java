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
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.managecase.repository.DefaultDataStoreRepository.ES_QUERY;

class DataStoreRepositoryTest {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String ASSIGNEE_ID = "0a5874a4-3f38-4bbd-ba4c";
    private static final String ROLE = "caseworker-probate";
    private static final String CASE_ID = "12345678";

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
        CaseDetails caseDetails = CaseDetails.builder()
                .caseTypeId(CASE_TYPE_ID)
                .reference(CASE_ID)
                .build();
        CaseSearchResponse response = new CaseSearchResponse(Lists.newArrayList(caseDetails));
        given(dataStoreApi.searchCases(anyString(), anyString())).willReturn(response);

        Optional<CaseDetails> result = repository.findCaseBy(CASE_TYPE_ID, CASE_ID);

        assertThat(result).get().isEqualTo(caseDetails);

        verify(dataStoreApi).searchCases(eq(CASE_TYPE_ID), eq(String.format(ES_QUERY, CASE_ID)));
    }

    @Test
    @DisplayName("Find case return no cases")
    void shouldReturnNoCaseForSearch() {
        CaseSearchResponse response = new CaseSearchResponse(Lists.newArrayList());
        given(dataStoreApi.searchCases(anyString(), anyString())).willReturn(response);

        Optional<CaseDetails> result = repository.findCaseBy(CASE_TYPE_ID, CASE_ID);

        assertThat(result).isEqualTo(Optional.empty());
    }

    @Test
    @DisplayName("Find case by caseTypeId and caseId")
    @SuppressWarnings("unchecked")
    void assignCase() {
        doNothing().when(dataStoreApi).assignCase(anyList());

        repository.assignCase(CASE_ID, ROLE, ASSIGNEE_ID);

        ArgumentCaptor<List<CaseUserRole>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataStoreApi).assignCase(captor.capture());
        List<CaseUserRole> caseUserRoles = captor.getValue();

        assertThat(caseUserRoles.size()).isEqualTo(1);
        CaseUserRole caseUserRole = caseUserRoles.get(0);

        assertThat(caseUserRole.getCaseId()).isEqualTo(CASE_ID);
        assertThat(caseUserRole.getCaseRole()).isEqualTo(ROLE);
        assertThat(caseUserRole.getUserId()).isEqualTo(ASSIGNEE_ID);
    }
}