package uk.gov.hmcts.reform.managecase.data.casedetails.supplementarydata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;


@Service
@Qualifier("default")
@Singleton
@Transactional
public class DefaultSupplementaryDataRepository implements SupplementaryDataRepository {

    @PersistenceContext
    private EntityManager em;

    private List<SupplementaryDataQueryBuilder> queryBuilders;

    @Autowired
    public DefaultSupplementaryDataRepository(final List<SupplementaryDataQueryBuilder> queryBuilders) {
        this.queryBuilders = queryBuilders;
    }

    @Override
    public void incrementSupplementaryData(final String caseReference,
                                           String fieldPath,
                                           Object fieldValue) {
        Query query = queryBuilder(SupplementaryDataOperation.INC).build(em,
                                                                         caseReference,
                                                                         fieldPath,
                                                                         fieldValue);
        query.executeUpdate();
    }

    private SupplementaryDataQueryBuilder queryBuilder(final SupplementaryDataOperation supplementaryDataOperation) {
        return this.queryBuilders.stream()
            .filter(builder -> builder.operationType() == supplementaryDataOperation)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Operation Type " + supplementaryDataOperation.getOperationName()
                                                        + " Not Supported"));
    }

}
