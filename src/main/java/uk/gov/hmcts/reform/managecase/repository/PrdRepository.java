package uk.gov.hmcts.reform.managecase.repository;

import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;

import java.util.Optional;

public interface PrdRepository {

    Optional<ProfessionalUser> findUserBy(String userIdentifier);
}
