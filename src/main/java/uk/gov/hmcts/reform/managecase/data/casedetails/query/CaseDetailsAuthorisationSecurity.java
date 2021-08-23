package uk.gov.hmcts.reform.managecase.data.casedetails.query;

import uk.gov.hmcts.reform.managecase.data.casedetails.search.MetaData;

public interface CaseDetailsAuthorisationSecurity {

    <T> void secure(CaseDetailsQueryBuilder<T> builder, MetaData metadata);

}
