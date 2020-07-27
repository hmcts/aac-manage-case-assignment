package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseSearchResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public static final String DUMMY_TITLE = "Dummy Title";

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
    public void assignCase(List<String> caseRoles, String caseId, String userId) {
        List<CaseUserRole> caseUserRoles = caseRoles.stream()
                .map(role -> CaseUserRole.builder().caseRole(role).caseId(caseId).userId(userId).build())
                .collect(Collectors.toList());
        dataStoreApi.assignCase(new CaseUserRoleResource(caseUserRoles));
    }

    @Override
    public List<CaseUserRole> getCaseAssignments(List<String> caseIds, List<String> userIds) {
        CaseUserRoleResource response = dataStoreApi.getCaseAssignments(caseIds, userIds);
        return response.getCaseUsers();
    }

    @Override
    public Map<String, String> getCaseTitles(List<String> caseIds) {
        // TODO : Design is still pending so hardcoded for now
        return caseIds.stream()
                .collect(Collectors.toMap(Function.identity(), caseId -> caseId + "-" + DUMMY_TITLE));
    }
}
