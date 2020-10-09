package uk.gov.hmcts.reform.managecase.repository;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDataContent;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseSearchResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRolesRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.datastore.Event;
import uk.gov.hmcts.reform.managecase.client.datastore.StartEventResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultViewResource;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.AvoidDuplicateLiterals"})
public class DefaultDataStoreRepository implements DataStoreRepository {

    static final String NOC_REQUEST_DESCRIPTION = "Notice of Change Request Event";

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDataStoreRepository.class);

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

    public static final String CHANGE_ORGANISATION_REQUEST = "ChangeOrganisationRequest";

    private final DataStoreApiClient dataStoreApi;
    private final JacksonUtils jacksonUtils;

    @Autowired
    public DefaultDataStoreRepository(DataStoreApiClient dataStoreApi,
                                      JacksonUtils jacksonUtils) {
        this.dataStoreApi = dataStoreApi;
        this.jacksonUtils = jacksonUtils;
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

    @Override
    public CaseResource submitEventForCase(String caseId,
                                           String eventId,
                                           ChangeOrganisationRequest changeOrganisationRequest) {

        CaseResource caseResource = null;
        StartEventResource startEventResource = dataStoreApi.getStartEventTrigger(caseId, eventId);

        if (startEventResource != null && startEventResource.getToken() != null) {
            Event event = Event.builder()
                .eventId(eventId)
                .description(NOC_REQUEST_DESCRIPTION)
                .build();

            CaseDataContent caseDataContent = CaseDataContent.builder()
                .token(startEventResource.getToken())
                .event(event)
                .data(getCaseDataContentData(changeOrganisationRequest))
                .build();

            caseResource = dataStoreApi.submitEventForCase(caseId, caseDataContent);
        } else {
            LOG.info("Failed to get token from start event");
        }

        return caseResource;
    }

    @Override
    public CaseResource findCaseByCaseIdExternalApi(String caseId) {
        return dataStoreApi.getCaseDetailsByCaseIdViaExternalApi(caseId);
    }

    private Map<String, JsonNode> getCaseDataContentData(ChangeOrganisationRequest changeOrganisationRequest) {
        Map<String, JsonNode> data = new HashMap<>();

        data.put(CHANGE_ORGANISATION_REQUEST, jacksonUtils.convertValue(changeOrganisationRequest, JsonNode.class));

        return data;
    }

}
