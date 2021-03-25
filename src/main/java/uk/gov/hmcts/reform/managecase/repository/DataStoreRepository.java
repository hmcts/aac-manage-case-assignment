package uk.gov.hmcts.reform.managecase.repository;

import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseEventCreationPayload;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.StartEventResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseUpdateViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;

import java.util.List;

public interface DataStoreRepository {

    void assignCase(List<String> caseRoles, String caseId, String userId, String organisationId);

    List<CaseUserRole> getCaseAssignments(List<String> caseIds, List<String> userIds);

    void removeCaseUserRoles(List<CaseUserRole> caseUserRoles, String organisationId);

    CaseViewResource findCaseByCaseId(String caseId);

    CaseDetails submitNoticeOfChangeRequestEvent(String caseId,
                                                 String eventId,
                                                 ChangeOrganisationRequest changeOrganisationRequest);

    CaseUpdateViewEvent getStartEventTrigger(String caseId, String eventId);

    StartEventResource getExternalStartEventTrigger(String caseId, String eventId);

    CaseDetails submitEventForCase(String caseId, CaseEventCreationPayload caseEventCreationPayload);

    CaseDetails findCaseByCaseIdExternalApi(String caseId);
}
