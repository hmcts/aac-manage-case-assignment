package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;

import java.util.List;

@Repository
public class DefaultDataStoreRepository implements DataStoreRepository {

    private static final String ES_QUERY = "{\n"
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
    public CaseDetails findCaseById(String caseId) {
        List<CaseDetails> caseDetails = dataStoreApi.searchCases(String.format(ES_QUERY, caseId));
        // TODO : size should be one otherwise throw error
        return caseDetails.get(0);
    }
}
