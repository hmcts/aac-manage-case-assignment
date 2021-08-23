package uk.gov.hmcts.reform.managecase.data.casedetails;

import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetailsExtended;

import java.util.Optional;

public interface CaseDetailsRepository {

    Optional<CaseDetailsExtended> findByReference(String jurisdiction, Long caseReference);

    Optional<CaseDetailsExtended> findByReference(String jurisdiction, String reference);

}
