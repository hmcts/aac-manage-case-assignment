package uk.gov.hmcts.reform.managecase.repository;

import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;

import java.util.List;

public interface DataStoreRepository {

    void assignCase(List<String> caseRoles, String caseId, String userId, String organisationId);

    List<CaseUserRole> getCaseAssignments(List<String> caseIds, List<String> userIds);

    void removeCaseUserRoles(List<CaseUserRole> caseUserRoles, String organisationId);

    CaseDetails findCaseByCaseIdExternalApi(String caseId);
}
