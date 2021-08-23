package uk.gov.hmcts.reform.managecase.data.casedetails;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetailsExtended;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Maps.newHashMap;

@Service
@Qualifier(CachedCaseDetailsRepository.QUALIFIER)
@RequestScope
public class CachedCaseDetailsRepository implements CaseDetailsRepository {

    private final CaseDetailsRepository caseDetailsRepository;
    public static final String QUALIFIER = "cached";
    private final Map<String, Optional<CaseDetailsExtended>> referenceToCaseDetails = newHashMap();

    @Inject
    public CachedCaseDetailsRepository(final @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
                                               CaseDetailsRepository caseDetailsRepository) {
        this.caseDetailsRepository = caseDetailsRepository;
    }

    @Override
    public Optional<CaseDetailsExtended> findByReference(String jurisdiction, Long reference) {
        return findByReference(jurisdiction, reference.toString());
    }

    @Override
    public Optional<CaseDetailsExtended> findByReference(String jurisdiction, String reference) {
        return referenceToCaseDetails.computeIfAbsent(reference, key ->
            caseDetailsRepository.findByReference(jurisdiction, reference));
    }
}
