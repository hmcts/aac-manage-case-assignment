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
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseUpdateViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewField;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.WizardPage;
import uk.gov.hmcts.reform.managecase.client.datastore.model.WizardPageComplexFieldOverride;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultViewResource;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository("defaultDataStoreRepository")
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.UseConcurrentHashMap", "PMD.AvoidDuplicateLiterals"})
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

    protected final DataStoreApiClient dataStoreApi;
    private final JacksonUtils jacksonUtils;
    protected final SecurityUtils securityUtils;

    @Autowired
    public DefaultDataStoreRepository(DataStoreApiClient dataStoreApi,
                                      JacksonUtils jacksonUtils,
                                      SecurityUtils securityUtils) {
        this.dataStoreApi = dataStoreApi;
        this.jacksonUtils = jacksonUtils;
        this.securityUtils = securityUtils;
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
        return dataStoreApi.getCaseDetailsByCaseId(getUserAuthToken(), caseId);
    }

    protected String getUserAuthToken() {
        return securityUtils.getCaaSystemUserToken();
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
    public CaseUpdateViewEvent getStartEventTrigger(String caseId, String eventId) {
        String userAuthToken = getUserAuthToken();
        return dataStoreApi.getStartEventTrigger(userAuthToken, caseId, eventId);
    }

    @Override
    public CaseResource submitEventForCaseOnly(String caseId, CaseDataContent caseDataContent) {
        String userAuthToken = getUserAuthToken();
        return dataStoreApi.submitEventForCase(userAuthToken, caseId, caseDataContent);
    }

    @Override
    public CaseResource submitEventForCase(String caseId,
                                           String eventId,
                                           ChangeOrganisationRequest changeOrganisationRequest) {

        CaseResource caseResource = null;
        String userAuthToken = getUserAuthToken();
        CaseUpdateViewEvent caseUpdateViewEvent = dataStoreApi.getStartEventTrigger(userAuthToken, caseId, eventId);

        if (caseUpdateViewEvent != null) {
            Optional<CaseViewField> caseViewField = getCaseViewField(caseUpdateViewEvent);

            if (caseViewField.isPresent()) {
                String caseFieldId = caseViewField.get().getId();

                String approvalStatusDefaultValue = getApprovalStatusDefaultValue(
                    caseUpdateViewEvent.getWizardPages(),
                    caseFieldId
                );

                if (caseUpdateViewEvent.getEventToken() == null || approvalStatusDefaultValue == null) {
                    LOG.info("Failed to get enough data from start event to submit an event for the case");
                } else {
                    Event event = Event.builder()
                        .eventId(eventId)
                        .description(NOC_REQUEST_DESCRIPTION)
                        .build();

                    changeOrganisationRequest.setApprovalStatus(approvalStatusDefaultValue);

                    CaseDataContent caseDataContent = CaseDataContent.builder()
                        .token(caseUpdateViewEvent.getEventToken())
                        .event(event)
                        .data(getCaseDataContentData(caseFieldId, changeOrganisationRequest))
                        .build();

                    caseResource = dataStoreApi.submitEventForCase(userAuthToken, caseId, caseDataContent);
                }
            } else {
                LOG.info("Failed to create ChangeOrganisationRequest because of missing case field id");
            }
        }
        return caseResource;
    }

    private String getApprovalStatusDefaultValue(List<WizardPage> wizardPages, String caseFieldId) {

        String returnValue = null;
        String approvalStatusFieldId = caseFieldId + ".ApprovalStatus";
        Optional<WizardPage> optionalWizardPage = wizardPages.stream().findFirst();

        if (optionalWizardPage.isPresent()) {
            WizardPage wizardPage = optionalWizardPage.get();
            Optional<WizardPageComplexFieldOverride> filteredWizardPageField =
                wizardPage.getWizardPageFields().stream()
                .filter(wizardPageField -> wizardPageField.getCaseFieldId().equals(caseFieldId)
                    && wizardPageField.getComplexFieldOverride(approvalStatusFieldId).isPresent())
                .map(wizardPageField -> wizardPageField.getComplexFieldOverride(approvalStatusFieldId).get())
                .findFirst();
            if (filteredWizardPageField.isPresent()) {
                returnValue = filteredWizardPageField.get().getDefaultValue();
            }
        }

        return returnValue;
    }

    private Optional<CaseViewField> getCaseViewField(CaseUpdateViewEvent caseUpdateViewEvent) {
        Optional<CaseViewField> caseViewField = Optional.empty();

        if (caseUpdateViewEvent.getCaseFields() != null) {
            caseViewField = caseUpdateViewEvent.getCaseFields().stream()
                .filter(cvf -> cvf.getFieldTypeDefinition().getId().equals(CHANGE_ORGANISATION_REQUEST))
                .findFirst();
        }
        return caseViewField;
    }

    @Override
    public CaseResource findCaseByCaseIdExternalApi(String caseId) {
        return dataStoreApi.getCaseDetailsByCaseIdViaExternalApi(caseId);
    }

    private Map<String, JsonNode> getCaseDataContentData(String caseFieldId,
                                                         ChangeOrganisationRequest changeOrganisationRequest) {
        Map<String, JsonNode> data = new HashMap<>();

        data.put(caseFieldId, jacksonUtils.convertValue(changeOrganisationRequest, JsonNode.class));

        return data;
    }

}
