package uk.gov.hmcts.reform.managecase.repository;

import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;

import java.util.List;
import java.util.Optional;

public interface DataStoreRepository {

    Optional<CaseDetails> findCaseBy(String caseTypeId, String caseId);

    void assignCase(List<String> caseRoles, String caseId, String userId, String organisationId);

    List<CaseUserRole> getCaseAssignments(List<String> caseIds, List<String> userIds);

    void removeCaseUserRoles(List<CaseUserRole> caseUserRoles, String organisationId);

}
