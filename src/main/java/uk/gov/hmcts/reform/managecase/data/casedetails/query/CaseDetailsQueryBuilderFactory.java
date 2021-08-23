package uk.gov.hmcts.reform.managecase.data.casedetails.query;

import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.managecase.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.reform.managecase.data.casedetails.search.MetaData;

import javax.persistence.EntityManager;
import java.util.List;

public class CaseDetailsQueryBuilderFactory {

    private final List<CaseDetailsAuthorisationSecurity> caseDetailsAuthorisationSecurities;

    @Autowired
    public CaseDetailsQueryBuilderFactory(List<CaseDetailsAuthorisationSecurity> caseDetailsAuthorisationSecurities) {
        this.caseDetailsAuthorisationSecurities = caseDetailsAuthorisationSecurities;
    }

    public CaseDetailsQueryBuilder<CaseDetailsEntity> selectSecured(EntityManager em) {
        return selectSecured(em, null);
    }

    public CaseDetailsQueryBuilder<CaseDetailsEntity> selectSecured(EntityManager em, MetaData metaData) {
        return secure(new SelectCaseDetailsQueryBuilder(em), metaData);
    }

    private <T> CaseDetailsQueryBuilder<T> secure(CaseDetailsQueryBuilder<T> builder, MetaData metadata) {
        caseDetailsAuthorisationSecurities.forEach(security -> security.secure(builder, metadata));
        return builder;
    }
}
