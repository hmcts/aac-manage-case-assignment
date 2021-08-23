package uk.gov.hmcts.reform.managecase.data.casedetails;

import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetailsExtended;
import uk.gov.hmcts.reform.managecase.data.casedetails.query.CaseDetailsQueryBuilder;
import uk.gov.hmcts.reform.managecase.data.casedetails.query.CaseDetailsQueryBuilderFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Optional;


@Named
@Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
@Singleton
@SuppressWarnings("checkstyle:SummaryJavadoc")
public class DefaultCaseDetailsRepository implements CaseDetailsRepository {

    public static final String QUALIFIER = "default";
    private final CaseDetailsMapper caseDetailsMapper;
    private final CaseDetailsQueryBuilderFactory queryBuilderFactory;

    @PersistenceContext
    private EntityManager em;

    @Inject
    public DefaultCaseDetailsRepository(
        final CaseDetailsMapper caseDetailsMapper, CaseDetailsQueryBuilderFactory queryBuilderFactory) {
        this.caseDetailsMapper = caseDetailsMapper;
        this.queryBuilderFactory = queryBuilderFactory;
    }

    @Override
    public Optional<CaseDetailsExtended> findByReference(String jurisdiction, Long caseReference) {
        return findByReference(jurisdiction, caseReference.toString());
    }

    @Override
    public Optional<CaseDetailsExtended> findByReference(String jurisdiction, String reference) {
        return find(jurisdiction, null, reference).map(this.caseDetailsMapper::entityToModel);
    }

    private Optional<CaseDetailsEntity> find(String jurisdiction, Long id, String reference) {
        final CaseDetailsQueryBuilder<CaseDetailsEntity> qb = queryBuilderFactory.selectSecured(em);

        if (null != jurisdiction) {
            qb.whereJurisdiction(jurisdiction);
        }

        return getCaseDetailsEntity(id, reference, qb);
    }

    private Optional<CaseDetailsEntity> getCaseDetailsEntity(Long id,
                                                             String reference,
                                                             CaseDetailsQueryBuilder<CaseDetailsEntity> qb) {
        if (null != reference) {
            qb.whereReference(reference);
        } else {
            qb.whereId(id);
        }

        return qb.getSingleResult();
    }
}
