package uk.gov.hmcts.reform.managecase.data.casedetails;

import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetailsExtended;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class CaseDetailsMapper {

    public CaseDetailsExtended entityToModel(final CaseDetailsEntity caseDetailsEntity) {
        if (caseDetailsEntity == null) {
            return null;
        }

        final CaseDetailsExtended caseDetails = new CaseDetailsExtended();
        caseDetails.setReference(caseDetailsEntity.getReference());
        caseDetails.setId(String.valueOf(caseDetailsEntity.getId()));
        caseDetails.setCaseTypeId(caseDetailsEntity.getCaseType());
        caseDetails.setJurisdiction(caseDetailsEntity.getJurisdiction());
        caseDetails.setCreatedDate(caseDetailsEntity.getCreatedDate());
        caseDetails.setLastStateModifiedDate(caseDetailsEntity.getLastStateModifiedDate());
        caseDetails.setLastModified(caseDetailsEntity.getLastModified());
        caseDetails.setState(caseDetailsEntity.getState());
        caseDetails.setSecurityClassification(caseDetailsEntity.getSecurityClassification());
        caseDetails.setVersion(caseDetailsEntity.getVersion());
        if (caseDetailsEntity.getData() != null) {
            caseDetails.setData(JacksonUtils.convertValue(caseDetailsEntity.getData()));
            caseDetails.setDataClassification(JacksonUtils.convertValue(caseDetailsEntity.getDataClassification()));
        }
        if (caseDetailsEntity.getSupplementaryData() != null) {
            caseDetails.setSupplementaryData(JacksonUtils.convertValue(caseDetailsEntity.getSupplementaryData()));
        }
        return caseDetails;
    }
}
