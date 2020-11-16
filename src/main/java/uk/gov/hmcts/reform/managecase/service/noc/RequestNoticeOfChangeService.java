package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseResource;
import uk.gov.hmcts.reform.managecase.client.datastore.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.service.noc.ApprovalStatus.APPROVED;
import static uk.gov.hmcts.reform.managecase.service.noc.ApprovalStatus.PENDING;

@Service
public class RequestNoticeOfChangeService {
    private final DataStoreRepository dataStoreRepository;
    private final PrdRepository prdRepository;
    private final JacksonUtils jacksonUtils;
    private final SecurityUtils securityUtils;

    @Autowired
    public RequestNoticeOfChangeService(NoticeOfChangeQuestions noticeOfChangeQuestions,
                                        DataStoreRepository dataStoreRepository,
                                        PrdRepository prdRepository,
                                        JacksonUtils jacksonUtils,
                                   SecurityUtils securityUtils) {
        this.dataStoreRepository = dataStoreRepository;
        this.prdRepository = prdRepository;
        this.jacksonUtils = jacksonUtils;
        this.securityUtils = securityUtils;
    }

    public RequestNoticeOfChangeResponse requestNoticeOfChange(NoCRequestDetails noCRequestDetails) {
        String caseId = noCRequestDetails.getCaseViewResource().getReference();

        Organisation incumbentOrganisation = noCRequestDetails.getOrganisationPolicy().getOrganisation();
        String caseRoleId = noCRequestDetails.getOrganisationPolicy().getOrgPolicyCaseAssignedRole();

        String organisationIdentifier = prdRepository.findUsersByOrganisation().getOrganisationIdentifier();

        Organisation invokersOrganisation = Organisation.builder().organisationID(organisationIdentifier).build();

        String eventId = getEventId(noCRequestDetails);

        generateNoCRequestEvent(caseId, invokersOrganisation, incumbentOrganisation, caseRoleId, eventId);

        // The case may have been changed as a result of the post-submit callback to CheckForNoCApproval operation.
        // Case data is therefore reloaded before checking if the NoCRequest has been auto-approved
        CaseResource caseResource = getCaseViaExternalApi(caseId);

        boolean isApprovalComplete =
            isNocRequestAutoApprovalCompleted(caseResource, invokersOrganisation, caseRoleId);

        // Auto-assign relevant case-roles to the invoker if required
        if (isApprovalComplete
            && isActingAsSolicitor(securityUtils.getUserInfo().getRoles(), caseResource.getJurisdiction())) {
            autoAssignCaseRoles(caseResource, invokersOrganisation);
        }

        return RequestNoticeOfChangeResponse.builder()
            .caseRole(caseRoleId)
            .approvalStatus(isApprovalComplete ? APPROVED : PENDING)
            .status(REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)
            .build();
    }

    private boolean isActingAsSolicitor(List<String> roles, String jurisdiction) {
        return securityUtils.hasSolicitorRole(roles, jurisdiction);
    }

    private CaseResource getCaseViaExternalApi(String caseId) {
        return dataStoreRepository.findCaseByCaseIdExternalApi(caseId);
    }

    private String getEventId(NoCRequestDetails noCRequestDetails) {
        // previous NoC related service calls
        // (https://tools.hmcts.net/confluence/display/ACA/API+Operation%3A+Get+NoC+Questions)
        // will have validated that events exist.
        // Assuming a single event, so always take first array element
        return noCRequestDetails.getCaseViewResource().getCaseViewActionableEvents()[0].getId();
    }

    private CaseResource generateNoCRequestEvent(String caseId,
                                                 Organisation invokersOrganisation,
                                                 Organisation incumbentOrganisation,
                                                 String caseRoleId,
                                                 String eventId) {
        ChangeOrganisationRequest changeOrganisationRequest = ChangeOrganisationRequest.builder()
            .caseRoleId(caseRoleId)
            .organisationToAdd(invokersOrganisation)
            .organisationToRemove(incumbentOrganisation)
            .requestTimestamp(LocalDateTime.now())
            .build();

        // Submit the NoCRequest event + event token.  This action will trigger a submitted callback to the
        // CheckForNoCApproval operation, which will apply additional processing in the event of auto-approval.
        return dataStoreRepository.submitEventForCase(caseId, eventId, changeOrganisationRequest);
    }

    private boolean isNocRequestAutoApprovalCompleted(CaseResource caseResource,
                                                      Organisation invokersOrganisation,
                                                      String caseRoleId) {
        Optional<ChangeOrganisationRequest> changeOrganisationRequest = getChangeOrganisationRequest(caseResource);

        return changeOrganisationRequest.isPresent()
            && changeOrganisationRequest.get().getCaseRoleId() == null
            && isRequestToAddOrReplaceRepresentationAndApproved(caseResource, invokersOrganisation, caseRoleId);
    }

    private Optional<ChangeOrganisationRequest> getChangeOrganisationRequest(CaseResource caseResource) {
        Optional changeOrganisationRequest = Optional.empty();
        final Optional<JsonNode> changeOrganisationRequestNode = caseResource.findChangeOrganisationRequestNode();

        if (changeOrganisationRequestNode.isPresent()) {
            changeOrganisationRequest = Optional.of(jacksonUtils.convertValue(changeOrganisationRequestNode.get(),
                                                                              ChangeOrganisationRequest.class));
        }

        return changeOrganisationRequest;
    }

    private void autoAssignCaseRoles(CaseResource caseResource,
                                     Organisation invokersOrganisation) {
        List<String> invokerOrgPolicyRoles =
            findInvokerOrgPolicyRoles(caseResource, invokersOrganisation);

        dataStoreRepository.assignCase(invokerOrgPolicyRoles, caseResource.getReference(),
                                       securityUtils.getUserInfo().getUid(), invokersOrganisation.getOrganisationID());
    }

    private boolean isRequestToAddOrReplaceRepresentationAndApproved(CaseResource caseResource,
                                                                     Organisation organisation,
                                                                     String caseRoleId) {
        return findInvokerOrgPolicyRoles(caseResource, organisation).contains(caseRoleId);
    }

    private List<OrganisationPolicy> findPolicies(CaseResource caseResource) {
        List<JsonNode> policyNodes = caseResource.findOrganisationPolicyNodes();
        return policyNodes.stream()
            .map(node -> jacksonUtils.convertValue(node, OrganisationPolicy.class))
            .collect(toList());
    }

    private List<String> findInvokerOrgPolicyRoles(CaseResource caseResource, Organisation organisation) {
        List<OrganisationPolicy> policies = findPolicies(caseResource);
        return policies.stream()
            .filter(policy -> policy.getOrganisation() != null
                && organisation.getOrganisationID().equalsIgnoreCase(policy.getOrganisation().getOrganisationID()))
            .map(OrganisationPolicy::getOrgPolicyCaseAssignedRole)
            .collect(toList());
    }
}
