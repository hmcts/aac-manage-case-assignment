package uk.gov.hmcts.reform.managecase.repository;

import com.fasterxml.jackson.databind.JsonNode;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseCouldNotBeFoundException;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseEventCreationPayload;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRolesRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.datastore.Event;
import uk.gov.hmcts.reform.managecase.client.datastore.StartEventResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseUpdateViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewField;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_NOT_FOUND;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.PENDING;

@Repository("defaultDataStoreRepository")
@SuppressWarnings({"PMD.PreserveStackTrace", "PMD.DataflowAnomalyAnalysis",
    "PMD.LawOfDemeter","PMD.DataflowAnomalyAnalysis",
    "PMD.UseConcurrentHashMap", "PMD.AvoidDuplicateLiterals"})
public class DefaultDataStoreRepository implements DataStoreRepository {

    static final String NOC_REQUEST_DESCRIPTION = "Notice of Change Request Event";

    static final String NOT_ENOUGH_DATA_TO_SUBMIT_START_EVENT = "Failed to get enough data from start event "
        + "to submit an event for the case";

    static final String CHANGE_ORGANISATION_REQUEST_MISSING_CASE_FIELD_ID = "Failed to create ChangeOrganisationRequest"
        + " because of missing case field id";

    public static final String CHANGE_ORGANISATION_REQUEST = "ChangeOrganisationRequest";
    public static final String INCOMPLETE_CALLBACK = "INCOMPLETE_CALLBACK";
    public static final String CALLBACK_FAILED_ERRORS_MESSAGE =
        "Submitted callback failed. Please check data-store-api logs";

    private final DataStoreApiClient dataStoreApi;
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
    public CaseViewResource findCaseByCaseId(String caseId) {
        try {
            return dataStoreApi.getCaseDetailsByCaseId(getUserAuthToken(), caseId);
        } catch (FeignException ex) {
            if (HttpStatus.NOT_FOUND.value() == ex.status()) {
                throw new CaseCouldNotBeFoundException(CASE_NOT_FOUND);
            }
            throw ex;
        }
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
    public StartEventResource getExternalStartEventTrigger(String caseId, String eventId) {
        String userAuthToken = getUserAuthToken();
        return dataStoreApi.getExternalStartEventTrigger(userAuthToken, caseId, eventId);
    }

    @Override
    public CaseDetails submitEventForCase(String caseId, CaseEventCreationPayload caseEventCreationPayload) {
        String userAuthToken = getUserAuthToken();
        return submitEvent(caseId, caseEventCreationPayload, userAuthToken);
    }

    @Override
    public CaseDetails submitNoticeOfChangeRequestEvent(String caseId,
                                                        String eventId,
                                                        ChangeOrganisationRequest changeOrganisationRequest) {

        CaseDetails caseDetails = null;
        String userAuthToken = getUserAuthToken();
        CaseUpdateViewEvent caseUpdateViewEvent = dataStoreApi.getStartEventTrigger(userAuthToken, caseId, eventId);

        if (caseUpdateViewEvent != null) {

            if (caseUpdateViewEvent.getEventToken() == null) {
                throw new IllegalStateException(NOT_ENOUGH_DATA_TO_SUBMIT_START_EVENT);
            }

            Optional<CaseViewField> caseViewField = getChangeOrganisationRequestCaseViewField(caseUpdateViewEvent);

            if (caseViewField.isPresent()) {
                String caseFieldId = caseViewField.get().getId();

                Event event = Event.builder()
                    .eventId(eventId)
                    .description(NOC_REQUEST_DESCRIPTION)
                    .build();

                StartEventResource startEventResource =
                    dataStoreApi.getExternalStartEventTrigger(userAuthToken, caseId, eventId);
                Map<String, JsonNode> caseData = startEventResource.getCaseDetails().getData();

                setChangeOrganisationRequestApprovalStatus(changeOrganisationRequest, caseFieldId, caseData);

                CaseEventCreationPayload caseEventCreationPayload = CaseEventCreationPayload.builder()
                    .token(caseUpdateViewEvent.getEventToken())
                    .event(event)
                    .onBehalfOfUserToken(securityUtils.getUserToken())
                    .data(getCaseDataContentData(caseFieldId, changeOrganisationRequest, caseData))
                    .build();

                caseDetails = submitEvent(caseId, caseEventCreationPayload, userAuthToken);
            } else {
                throw new IllegalStateException(CHANGE_ORGANISATION_REQUEST_MISSING_CASE_FIELD_ID);
            }
        }
        return caseDetails;
    }

    private void setChangeOrganisationRequestApprovalStatus(ChangeOrganisationRequest changeOrganisationRequest,
                                                            String caseFieldId,
                                                            Map<String, JsonNode> caseData) {
        JsonNode defaultApprovalStatusValue = caseData.get(caseFieldId);

        if (defaultApprovalStatusValue == null
            || defaultApprovalStatusValue.isMissingNode()
            || defaultApprovalStatusValue.isEmpty()) {
            changeOrganisationRequest.setApprovalStatus(PENDING.getValue());
        }
    }

    private Optional<CaseViewField> getChangeOrganisationRequestCaseViewField(CaseUpdateViewEvent caseUpdateViewEvent) {
        Optional<CaseViewField> changeOrgRequestCaseViewField = Optional.empty();

        if (caseUpdateViewEvent.getCaseFields() != null) {
            changeOrgRequestCaseViewField = caseUpdateViewEvent.getCaseFields().stream()
                .filter(cvf -> cvf.getFieldTypeDefinition().getId().equals(CHANGE_ORGANISATION_REQUEST))
                .findFirst();
        }
        return changeOrgRequestCaseViewField;
    }

    @Override
    public CaseDetails findCaseByCaseIdUsingExternalApi(String caseId) {
        return findCaseByCaseIdExternalApi(caseId, false);
    }

    @Override
    public CaseDetails findCaseByCaseIdAsSystemUserUsingExternalApi(String caseId) {
        return findCaseByCaseIdExternalApi(caseId, true);
    }

    private CaseDetails findCaseByCaseIdExternalApi(String caseId, boolean asSystemUser) {
        try {
            if (asSystemUser) {
                return dataStoreApi.getCaseDetailsByCaseIdViaExternalApi(securityUtils.getCaaSystemUserToken(), caseId);
            } else {
                return dataStoreApi.getCaseDetailsByCaseIdViaExternalApi(securityUtils.getUserToken(), caseId);
            }
        } catch (FeignException e) {
            if (HttpStatus.NOT_FOUND.value() == e.status()) {
                throw new CaseCouldNotBeFoundException(CASE_NOT_FOUND);
            }
            throw e;
        }
    }

    private Map<String, JsonNode> getCaseDataContentData(String caseFieldId,
                                                         ChangeOrganisationRequest changeOrganisationRequest,
                                                         Map<String, JsonNode> caseDetailsData) {
        Map<String, JsonNode> data = new HashMap<>();

        data.put(caseFieldId, jacksonUtils.convertValue(changeOrganisationRequest, JsonNode.class));

        jacksonUtils.merge(data, caseDetailsData);
        return caseDetailsData;
    }

    private CaseDetails submitEvent(String caseId, CaseEventCreationPayload caseEventCreationPayload,
                                    String userAuthToken) {
        CaseDetails caseDetails = dataStoreApi.submitEventForCase(userAuthToken, caseId, caseEventCreationPayload);
        if (INCOMPLETE_CALLBACK.equalsIgnoreCase(caseDetails.getCallbackResponseStatus())) {
            throw new RuntimeException(CALLBACK_FAILED_ERRORS_MESSAGE);
        }
        return caseDetails;
    }

}
