package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseSearchResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;

import java.util.List;
import java.util.Optional;

@Repository
public class DefaultDataStoreRepository implements DataStoreRepository {

    public static final String ES_QUERY = "{\n"
        + "   \"query\":{\n"
        + "      \"bool\":{\n"
        + "         \"filter\":{\n"
        + "            \"term\":{\n"
        + "               \"reference\":%s\n"
        + "            }\n"
        + "         }\n"
        + "      }\n"
        + "   }\n"
        + "}";

    private final DataStoreApiClient dataStoreApi;

    @Autowired
    public DefaultDataStoreRepository(DataStoreApiClient dataStoreApi) {
        this.dataStoreApi = dataStoreApi;
    }

    @Override
    public Optional<CaseDetails> findCaseBy(String caseTypeId, String caseId) {
        CaseSearchResponse searchResponse = dataStoreApi.searchCases(caseTypeId, String.format(ES_QUERY, caseId));
        return searchResponse.getCases().stream().findFirst();
    }

    @Override
    public void assignCase(String caseId, String caseRole, String userId) {
        CaseUserRole caseUserRole = CaseUserRole.builder()
            .caseRole(caseRole)
            .caseId(caseId)
            .userId(userId)
            .build();
        dataStoreApi.assignCase(List.of(caseUserRole));
    }
}
