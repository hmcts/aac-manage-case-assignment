package uk.gov.hmcts.reform.managecase.repository;

import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DataStoreRepository {

    Optional<CaseDetails> findCaseBy(String caseTypeId, String caseId);

    void assignCase(List<String> caseRoles, String caseId, String userId);

    List<CaseUserRole> getCaseAssignments(List<String> caseIds, List<String> userIds);

    Map<String, String> getCaseTitles(List<String> caseIds);
}
