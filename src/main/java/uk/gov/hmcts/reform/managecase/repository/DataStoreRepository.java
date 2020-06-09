package uk.gov.hmcts.reform.managecase.repository;

import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;

import java.util.Optional;

public interface DataStoreRepository {

    Optional<CaseDetails> findCaseBy(String caseTypeId, String caseId);

    void assignCase(String caseId, String caseRole, String userId);
}
