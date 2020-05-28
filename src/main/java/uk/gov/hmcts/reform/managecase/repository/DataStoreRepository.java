package uk.gov.hmcts.reform.managecase.repository;

import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;

public interface DataStoreRepository {

    CaseDetails findCaseById(String caseId);
}
