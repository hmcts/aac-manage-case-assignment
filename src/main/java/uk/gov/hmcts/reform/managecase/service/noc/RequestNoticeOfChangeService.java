package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeResponse;
import uk.gov.hmcts.reform.managecase.api.payload.SetOrganisationToRemoveResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CallbackCaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseResource;
import uk.gov.hmcts.reform.managecase.client.datastore.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.INVALID_CASE_ROLE_FIELD;

@Service
public class RequestNoticeOfChangeService {

    private static final String APPROVED = "APPROVED";
    private static final String PENDING = "PENDING";

    private final DataStoreRepository dataStoreRepository;
    private final PrdRepository prdRepository;
    private final JacksonUtils jacksonUtils;
    private final SecurityUtils securityUtils;

    @Autowired
    public RequestNoticeOfChangeService(NoticeOfChangeQuestions noticeOfChangeQuestions,
                                        @Qualifier("defaultDataStoreRepository")
                                            DataStoreRepository dataStoreRepository,
                                        PrdRepository prdRepository,
                                        JacksonUtils jacksonUtils,
                                        SecurityUtils securityUtils) {
        this.dataStoreRepository = dataStoreRepository;
        this.prdRepository = prdRepository;
        this.jacksonUtils = jacksonUtils;
        this.securityUtils = securityUtils;
    }

    /**
     * Input parameters to be decided on with Dan, dependant on response from NocAnswers.
     *
     * @param noCRequestDetails details of the NoCRequest provided by the call ot NocAnswers
     * @return RequestNoticeOfChangeResponse
     */
    public RequestNoticeOfChangeResponse requestNoticeOfChange(NoCRequestDetails noCRequestDetails) {
        String caseId = noCRequestDetails.getCaseViewResource().getReference();

        Organisation incumbentOrganisation = noCRequestDetails.getOrganisationPolicy().getOrganisation();
        String caseRoleId = noCRequestDetails.getOrganisationPolicy().getOrgPolicyCaseAssignedRole();

        String organisationIdentifier = prdRepository.findUsersByOrganisation().getOrganisationIdentifier();

        Organisation invokersOrganisation = Organisation.builder().organisationID(organisationIdentifier).build();

        String eventId = getEventId(noCRequestDetails);

        // LLD Step 4: Generate the NoCRequest event:
        generateNoCRequestEvent(caseId, invokersOrganisation, incumbentOrganisation, caseRoleId, eventId);

        // LLD Step 5: Confirm if the NoCRequest has been auto-approved:
        // Reload the case data (EI-3); as the case may have been changed as a result of the post-submit callback to
        // CheckForNoCApproval operation, return error if detected (Response case - 7).
        CaseResource caseResource = getCaseViaExternalApi(caseId);

        boolean isApprovalComplete =
            isNocRequestAutoApprovalCompleted(caseResource, invokersOrganisation, caseRoleId);

        // LLD STep 6: Auto-assign relevant case-roles to the invoker if required, i.e.:
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

    public SetOrganisationToRemoveResponse setOrganisationToRemove(
        CallbackCaseDetails caseDetails, ChangeOrganisationRequest changeOrganisationRequest) {

        List<JsonNode> organisationPolicyNodes = caseDetails.findOrganisationPolicyNodes();
        List<OrganisationPolicy> matchingOrganisationPolicyNodes =
            organisationPolicyNodes.stream()
                .map(node -> jacksonUtils.convertValue(node, OrganisationPolicy.class))
                .filter(orgPolicy ->
                            orgPolicy.getOrgPolicyCaseAssignedRole()
                                .equalsIgnoreCase(changeOrganisationRequest.getCaseRoleId()))
                .collect(toList());

        if (matchingOrganisationPolicyNodes.size() != 1) {
            throw new ValidationException(INVALID_CASE_ROLE_FIELD);
        }

        changeOrganisationRequest
            .setOrganisationToRemove(matchingOrganisationPolicyNodes.get(0).getOrganisation());

        Map<String, JsonNode> data = new HashMap<>(caseDetails.getData());
        data.put(caseDetails.getKeyFromDataWithValue(caseDetails.findChangeOrganisationRequestNode().get()),
                 jacksonUtils.convertValue(changeOrganisationRequest, JsonNode.class));

        return SetOrganisationToRemoveResponse.builder()
            .data(data)
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

        return dataStoreRepository.submitEventForCase(caseId, eventId, changeOrganisationRequest);

        //        Generate the NoCRequest event:
        //        Call EI-1 to generate an event token for a NoCRequest event for the case, return error if detected
        //        (Response case - 7).
        //        Update the following ChangeOrganisationRequest fields and in doing so confirm that the outcome of the
        //        request is the same as requested in the supplied expected outcome
        //        OrganisationToAdd = Organisation of the invoker as identified in Step-2.
        //        OrganisationToRemove = the incumbent organisation as identified in Step-2.
        //        CaseRole = the CaseRole in the OrganisationPolicy as identified in Step-2.
        //        RequestTimestamp = now.
        //            Reason = as specified in the request (may be null if allowed by config).
        //        Call EI-2 to submit the NoCRequest event + event token, return error if detected (Response case - 7).
        //... this action will trigger a submitted callback to the CheckForNoCApproval operation, which will apply
        // additional processing in the event of auto-approval.
    }

    private boolean isNocRequestAutoApprovalCompleted(CaseResource caseResource,
                                                      Organisation invokersOrganisation,
                                                      String caseRoleId) {
        //        Confirm if the following are all true:
        //        ChangeOrganisationRequest.CaseRole is NULL – i.e. the request has been auto-approved and processed.
        //        ChangeOrganisationRequest.ApprovalStatus is NULL - i.e. NULL (approved) or 0 (pending).
        //            The organisation in the OrganisationPolicy identified by the CaseRole saved in Step-2 matches
        //            that of the organisation of the invoker (also found in Step-2) – i.e. the request was to add or
        //            replace representation and it was approved.
        //
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
        //        From the case data reloaded in Step-5: find the Case Roles in any OrganisationPolicy associated with
        //        the Organisation of the invoker as identified in Step-2.
        //        Make a single call to EI-4 containing a list item for each case-role identified: holding
        //            case_id = as specified in the request
        //        user_id = invoker's IDAM ID
        //        case_role = case role as identified
        //        organisation_id = Organisation of the invoker as identified in Step-2.
        //... return error if detected (Response case - 7).
        List<String> invokerOrgPolicyRoles =
            findInvokerOrgPolicyRoles(caseResource, invokersOrganisation);

        dataStoreRepository.assignCase(invokerOrgPolicyRoles, caseResource.getReference(),
                                       securityUtils.getUserInfo().getUid(), invokersOrganisation.getOrganisationID());
    }

    private boolean isRequestToAddOrReplaceRepresentationAndApproved(CaseResource caseResource,
                                                                     Organisation organisation,
                                                                     String caseRoleId) {
        // 1. get all org policies from the caseResource
        // 2. find org policy that has case role is matching caseRoleId
        //3. check that organisation for that org policy matches the invokersOrganisation - match only on id
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
