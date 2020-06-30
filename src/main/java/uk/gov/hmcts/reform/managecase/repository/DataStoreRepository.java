package uk.gov.hmcts.reform.managecase.repository;

import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;

import java.util.Optional;

public interface DataStoreRepository {

    Optional<CaseDetails> findCaseBy(String caseTypeId, Long caseId);

    void assignCase(Long caseId, String caseRole, String userId);
}
