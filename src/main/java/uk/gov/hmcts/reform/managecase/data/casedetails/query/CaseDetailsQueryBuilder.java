package uk.gov.hmcts.reform.managecase.data.casedetails.query;

import uk.gov.hmcts.reform.managecase.data.casedetails.CaseDetailsEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class CaseDetailsQueryBuilder<T> {

    protected final EntityManager em;
    protected final CriteriaBuilder cb;
    protected final CriteriaQuery<T> query;
    protected final Root<CaseDetailsEntity> root;
    protected final List<Predicate> predicates;
    protected final List<Order> orders;

    CaseDetailsQueryBuilder(EntityManager em) {
        this.em = em;
        cb = em.getCriteriaBuilder();
        query = createQuery();
        root = query.from(CaseDetailsEntity.class);
        predicates = new ArrayList<>();
        orders = new ArrayList<>();

    }

    public abstract TypedQuery<T> build();

    public Optional<T> getSingleResult() {
        return build().getResultList()
            .stream()
            .findFirst();
    }

    protected abstract CriteriaQuery<T> createQuery();

    public CaseDetailsQueryBuilder whereJurisdiction(String jurisdiction) {
        predicates.add(cb.equal(root.get("jurisdiction"), jurisdiction));

        return this;
    }

    public CaseDetailsQueryBuilder whereReference(String reference) {
        predicates.add(cb.equal(root.get("reference"), reference));

        return this;
    }

    public CaseDetailsQueryBuilder whereId(Long id) {
        predicates.add(cb.equal(root.get("id"), id));

        return this;
    }
}

