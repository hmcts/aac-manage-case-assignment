package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.payload.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.DynamicList;
import uk.gov.hmcts.reform.managecase.domain.DynamicListElement;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.INVALID_CASE_ROLE_FIELD;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.APPROVED;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.PENDING;

@Service
public class RequestNoticeOfChangeService {

    public static final String MISSING_COR_CASE_ROLE_ID_IN_CASE_DEFINITION
        = "Missing ChangeOrganisationRequest.CaseRoleID %s in the case definition";
    private static final String ZERO = "0";
    private static final String USER_ID = ZERO;
    private static final String JURISDICTION = ZERO;

    private final DataStoreRepository dataStoreRepository;
    private final DefinitionStoreRepository definitionStoreRepository;
    private final PrdRepository prdRepository;
    private final JacksonUtils jacksonUtils;
    private final SecurityUtils securityUtils;

    @Autowired
    public RequestNoticeOfChangeService(NoticeOfChangeQuestions noticeOfChangeQuestions,
                                        @Qualifier("defaultDataStoreRepository")
                                            DataStoreRepository dataStoreRepository,
                                        DefinitionStoreRepository definitionStoreRepository,
                                        PrdRepository prdRepository,
                                        JacksonUtils jacksonUtils,
                                        SecurityUtils securityUtils) {
        this.dataStoreRepository = dataStoreRepository;
        this.definitionStoreRepository = definitionStoreRepository;
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

        String caseTypeId = noCRequestDetails.getCaseViewResource().getCaseType().getId();
        generateNoCRequestEvent(caseId, invokersOrganisation, incumbentOrganisation, caseRoleId, eventId, caseTypeId);

        // The case may have been changed as a result of the post-submit callback to CheckForNoCApproval operation.
        // Case data is therefore reloaded before checking if the NoCRequest has been auto-approved
        CaseDetails caseDetails = getCaseViaExternalApi(caseId);

        boolean isApprovalComplete =
            isNocRequestAutoApprovalCompleted(caseDetails, invokersOrganisation, caseRoleId);

        // Auto-assign relevant case-roles to the invoker if required
        if (isApprovalComplete
            && isActingAsSolicitor(securityUtils.getUserInfo().getRoles(), caseDetails.getJurisdiction())) {
            autoAssignCaseRoles(caseDetails, invokersOrganisation);
        }

        return RequestNoticeOfChangeResponse.builder()
            .caseRole(caseRoleId)
            .approvalStatus(isApprovalComplete ? APPROVED : PENDING)
            .status(REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)
            .build();
    }

    public AboutToSubmitCallbackResponse setOrganisationToRemove(CaseDetails caseDetails,
                                                                 ChangeOrganisationRequest changeOrganisationRequest,
                                                                 String changeOrganisationKey) {

        List<JsonNode> organisationPolicyNodes = caseDetails.findOrganisationPolicyNodes();
        List<OrganisationPolicy> matchingOrganisationPolicyNodes =
            organisationPolicyNodes.stream()
                .map(node -> jacksonUtils.convertValue(node, OrganisationPolicy.class))
                .filter(orgPolicy ->
                            orgPolicy.getOrgPolicyCaseAssignedRole()
                                .equalsIgnoreCase(changeOrganisationRequest.getCaseRoleId().getValue().getCode()))
                .collect(toList());

        if (matchingOrganisationPolicyNodes.size() != 1) {
            throw new ValidationException(INVALID_CASE_ROLE_FIELD);
        }

        changeOrganisationRequest
            .setOrganisationToRemove(matchingOrganisationPolicyNodes.get(0).getOrganisation());

        Map<String, JsonNode> data = new HashMap<>(caseDetails.getData());
        data.put(changeOrganisationKey, jacksonUtils.convertValue(changeOrganisationRequest, JsonNode.class));

        return AboutToSubmitCallbackResponse.builder()
            .data(data)
            .build();
    }

    private boolean isActingAsSolicitor(List<String> roles, String jurisdiction) {
        return securityUtils.hasSolicitorAndJurisdictionRoles(roles, jurisdiction);
    }

    private CaseDetails getCaseViaExternalApi(String caseId) {
        return dataStoreRepository.findCaseByCaseIdExternalApi(caseId);
    }

    private String getEventId(NoCRequestDetails noCRequestDetails) {
        // previous NoC related service calls
        // (https://tools.hmcts.net/confluence/display/ACA/API+Operation%3A+Get+NoC+Questions)
        // will have validated that events exist.
        // Assuming a single event, so always take first array element
        return noCRequestDetails.getCaseViewResource().getCaseViewActionableEvents()[0].getId();
    }

    private CaseDetails generateNoCRequestEvent(String caseId,
                                                Organisation invokersOrganisation,
                                                Organisation incumbentOrganisation,
                                                String caseRoleId,
                                                String eventId,
                                                String caseTypeId) {
        ChangeOrganisationRequest changeOrganisationRequest = ChangeOrganisationRequest.builder()
            .caseRoleId(createCaseRoleIdDynamicList(caseRoleId, caseTypeId))
            .organisationToAdd(invokersOrganisation)
            .organisationToRemove(incumbentOrganisation)
            .requestTimestamp(LocalDateTime.now())
            .build();

        // Submit the NoCRequest event + event token.  This action will trigger a submitted callback to the
        // CheckForNoCApproval operation, which will apply additional processing in the event of auto-approval.
        return dataStoreRepository.submitNoticeOfChangeRequestEvent(caseId, eventId, changeOrganisationRequest);
    }

    private DynamicList createCaseRoleIdDynamicList(String caseRoleId, String caseTypeId) {

        CaseRole caseRole = getCaseRolesDefinitions(caseRoleId, caseTypeId);
        DynamicList returnValue = null;
        if (caseRole != null) {
            DynamicListElement element = DynamicListElement.builder()
                .code(caseRole.getId())
                .label(caseRole.getName())
                .build();
            returnValue = DynamicList.builder()
                .value(element)
                .listItems(List.of(element))
                .build();
        }

        return returnValue;
    }

    private CaseRole getCaseRolesDefinitions(String caseRole, String caseType) {
        String caseRoleLowerCase = caseRole.toLowerCase();
        List<CaseRole> caseRolesDefinition = definitionStoreRepository.caseRoles(USER_ID, JURISDICTION, caseType);

        return caseRolesDefinition.stream()
            .filter(cr -> caseRoleLowerCase.equalsIgnoreCase(cr.getId().toLowerCase()))
            .findFirst()
            .orElseThrow(
                () -> new ValidationException(format(MISSING_COR_CASE_ROLE_ID_IN_CASE_DEFINITION, caseRole))
            );
    }

    private boolean isNocRequestAutoApprovalCompleted(CaseDetails caseDetails,
                                                      Organisation invokersOrganisation,
                                                      String caseRoleId) {
        Optional<ChangeOrganisationRequest> changeOrganisationRequest = getChangeOrganisationRequest(caseDetails);

        return changeOrganisationRequest.isPresent()
            && changeOrganisationRequest.get().getCaseRoleId() == null
            && isRequestToAddOrReplaceRepresentationAndApproved(caseDetails, invokersOrganisation, caseRoleId);
    }

    private Optional<ChangeOrganisationRequest> getChangeOrganisationRequest(CaseDetails caseDetails) {
        Optional<ChangeOrganisationRequest> changeOrganisationRequest = Optional.empty();
        final Optional<JsonNode> changeOrganisationRequestNode = caseDetails.findChangeOrganisationRequestNode();

        if (changeOrganisationRequestNode.isPresent()) {
            changeOrganisationRequest = Optional.of(jacksonUtils.convertValue(changeOrganisationRequestNode.get(),
                                                                              ChangeOrganisationRequest.class));
        }

        return changeOrganisationRequest;
    }

    private void autoAssignCaseRoles(CaseDetails caseDetails,
                                     Organisation invokersOrganisation) {
        List<String> invokerOrgPolicyRoles =
            findInvokerOrgPolicyRoles(caseDetails, invokersOrganisation);

        dataStoreRepository.assignCase(invokerOrgPolicyRoles, caseDetails.getId(),
                                       securityUtils.getUserInfo().getUid(), invokersOrganisation.getOrganisationID());
    }

    private boolean isRequestToAddOrReplaceRepresentationAndApproved(CaseDetails caseDetails,
                                                                     Organisation organisation,
                                                                     String caseRoleId) {
        return findInvokerOrgPolicyRoles(caseDetails, organisation).contains(caseRoleId);
    }

    private List<OrganisationPolicy> findPolicies(CaseDetails caseDetails) {
        List<JsonNode> policyNodes = caseDetails.findOrganisationPolicyNodes();
        return policyNodes.stream()
            .map(node -> jacksonUtils.convertValue(node, OrganisationPolicy.class))
            .collect(toList());
    }

    private List<String> findInvokerOrgPolicyRoles(CaseDetails caseDetails, Organisation organisation) {
        List<OrganisationPolicy> policies = findPolicies(caseDetails);
        return policies.stream()
            .filter(policy -> policy.getOrganisation() != null
                && organisation.getOrganisationID().equalsIgnoreCase(policy.getOrganisation().getOrganisationID()))
            .map(OrganisationPolicy::getOrgPolicyCaseAssignedRole)
            .collect(toList());
    }
}
