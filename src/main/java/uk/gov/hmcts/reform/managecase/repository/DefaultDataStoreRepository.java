package uk.gov.hmcts.reform.managecase.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseSearchResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRolesRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultViewResource;

import java.util.List;
import java.util.Optional;
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
    public CaseSearchResultViewResource findCaseBy(String caseTypeId, Optional<String> useCase, String caseId) {
        return dataStoreApi.internalSearchCases(caseTypeId,null, String.format(ES_QUERY, caseId));
    }

    @Override
    public CaseViewResource findCaseByCaseId(String caseId) {
        return dataStoreApi.getCaseDetailsByCaseId(caseId);
    }

    @Override
    public void assignCase(List<String> caseRoles, String caseId, String userId, String organisationId) {
        List<CaseUserRoleWithOrganisation> caseUserRoles = caseRoles.stream()
                .map(role -> CaseUserRoleWithOrganisation.withOrganisationBuilder()
                    .caseRole(role).caseId(caseId).userId(userId).organisationId(organisationId).build())
                .collect(Collectors.toList());
        dataStoreApi.assignCase(new CaseUserRolesRequest(caseUserRoles));
    }

    @Override
    public List<CaseUserRole> getCaseAssignments(List<String> caseIds, List<String> userIds) {
        CaseUserRoleResource response = dataStoreApi.getCaseAssignments(caseIds, userIds);
        return response.getCaseUsers();
    }

    @Override
    public void removeCaseUserRoles(List<CaseUserRole> caseUserRoles, String organisationId) {
        List<CaseUserRoleWithOrganisation> caseUsers = caseUserRoles.stream()
            .map(caseUserRole -> CaseUserRoleWithOrganisation.withOrganisationBuilder()
                .caseRole(caseUserRole.getCaseRole())
                .caseId(caseUserRole.getCaseId())
                .userId(caseUserRole.getUserId())
                .organisationId(organisationId)
                .build())
            .collect(Collectors.toList());
        dataStoreApi.removeCaseUserRoles(new CaseUserRolesRequest(caseUsers));
    }

}
