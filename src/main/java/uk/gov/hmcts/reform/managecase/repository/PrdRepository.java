package uk.gov.hmcts.reform.managecase.repository;

import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;

import java.util.List;

public interface PrdRepository {

    List<ProfessionalUser> findUsersByOrganisation();
}
