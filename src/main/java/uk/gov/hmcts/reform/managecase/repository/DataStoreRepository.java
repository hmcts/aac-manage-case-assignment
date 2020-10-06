package uk.gov.hmcts.reform.managecase.repository;

import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultViewResource;

import java.util.List;
import java.util.Optional;

public interface DataStoreRepository {

    Optional<CaseDetails> findCaseBy(String caseTypeId, String caseId);

    CaseSearchResultViewResource findCaseBy(String caseTypeId, Optional<String> useCase, String caseId);

    void assignCase(List<String> caseRoles, String caseId, String userId, String organisationId);

    List<CaseUserRole> getCaseAssignments(List<String> caseIds, List<String> userIds);

    void removeCaseUserRoles(List<CaseUserRole> caseUserRoles, String organisationId);

    CaseViewResource findCaseByCaseId(String caseId);

    CaseResource submitEventForCase(String caseId, String eventId, ChangeOrganisationRequest changeOrganisationRequest);

    CaseResource findCaseByCaseIdExternalApi(String caseId);
}
