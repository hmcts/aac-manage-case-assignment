package uk.gov.hmcts.reform.managecase.service.common;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;

@Service
public class CallerOrganisationService {

    private final PrdRepository prdRepository;

    public CallerOrganisationService(PrdRepository prdRepository) {
        this.prdRepository = prdRepository;
    }

    public String getCallerOrganisationId() {
        return prdRepository.findUsersByOrganisation().getOrganisationIdentifier();
    }
}
