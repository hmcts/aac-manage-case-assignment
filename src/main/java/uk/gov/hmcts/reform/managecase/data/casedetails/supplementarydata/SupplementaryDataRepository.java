package uk.gov.hmcts.reform.managecase.data.casedetails.supplementarydata;

public interface SupplementaryDataRepository {

    void incrementSupplementaryData(String caseReference, String fieldPath, Object fieldValue);

}
