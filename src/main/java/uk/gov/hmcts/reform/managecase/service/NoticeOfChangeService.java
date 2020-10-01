package uk.gov.hmcts.reform.managecase.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeResponse;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseResource;
import uk.gov.hmcts.reform.managecase.client.datastore.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewHeader;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeAnswer;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;

import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.managecase.api.controller.NoticeOfChangeController.REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE;

@Service
public class NoticeOfChangeService {

    public static final String PUI_ROLE = "pui-caa";
    public static final String NOC_EVENTS = "NOC";
    public static final String CHANGE_ORG_REQUEST = "ChangeOrganisationRequest";
    private static final int JURISDICTION_INDEX = 1;
    private static final String APPROVED = "APPROVED";
    private static final String PENDING = "PENDING";
    public static final String NOC_REQUEST = "NoCRequest";


    private final DataStoreRepository dataStoreRepository;
    private final DefinitionStoreRepository definitionStoreRepository;
    private final IdamRepository idamRepository;
    private final ChallengeQuestionService challengeQuestionService;
    private final PrdRepository prdRepository;

    @Autowired
    public NoticeOfChangeService(DataStoreRepository dataStoreRepository,
                                 IdamRepository idamRepository,
                                 DefinitionStoreRepository definitionStoreRepository,
                                 ChallengeQuestionService challengeQuestionService,
                                 PrdRepository prdRepository) {
        this.dataStoreRepository = dataStoreRepository;
        this.idamRepository = idamRepository;
        this.definitionStoreRepository = definitionStoreRepository;
        this.challengeQuestionService = challengeQuestionService;
        this.prdRepository = prdRepository;
    }

    public ChallengeQuestionsResult getChallengeQuestions(String caseId) {
        ChallengeQuestionsResult challengeQuestionsResult = challengeQuestions(caseId).getChallengeQuestionsResult();
        //Step 12 Remove the answer section from the JSON returned byGetTabContents and return success with the
        // remaining JSON
        challengeQuestionsResult.getQuestions().forEach(challengeQuestion -> {
            if (challengeQuestion.getAnswerField() != null) {
                challengeQuestion.setAnswers(new ArrayList<ChallengeAnswer>());
            }
        });
        return challengeQuestionsResult;
    }

    public NoCRequestDetails verifyNoticeOfChangeAnswers(VerifyNoCAnswersRequest verifyNoCAnswersRequest) {
        String caseId = verifyNoCAnswersRequest.getCaseId();
        NoCRequestDetails noCRequestDetails = challengeQuestions(caseId);
        SearchResultViewItem caseResult = noCRequestDetails.getSearchResultViewItem();

        String caseRoleId = challengeQuestionService
            .getMatchingCaseRole(noCRequestDetails.getChallengeQuestionsResult(),
                verifyNoCAnswersRequest.getAnswers(), caseResult);

        OrganisationPolicy organisationPolicy = findOrganisationPolicyForRole(caseResult, caseRoleId)
            .orElseThrow(() -> new ValidationException(String.format(
                "No OrganisationPolicy exists on the case for the case role '%s'", caseRoleId)));

        if (organisationEqualsRequestingUsers(organisationPolicy.getOrganisation())) {
            throw new ValidationException("The requestor has answered questions uniquely identifying"
                + " a litigant that they are already representing");
        }

        noCRequestDetails.setOrganisationPolicy(organisationPolicy);
        return noCRequestDetails;
    }

    public NoCRequestDetails challengeQuestions(String caseId) {
        //step 2 getCaseUsingGET(case Id) return error if case # invalid/not found
        CaseViewResource caseViewResource = getCase(caseId);
        //step 3 Check to see what events are available on the case (the system user with IdAM Role caseworker-caa only
        // has access to NoC events).  If no events are available, return an error
        getCaseEvents(caseViewResource);
        //step 4 Check the ChangeOrganisationRequest.CaseRole in the case record.  If it is not null, return an error
        // indicating that there is an ongoing NoCRequest.
        CaseSearchResultViewResource caseFields = findCaseBy(caseViewResource.getCaseType().getName(), caseId);
        getCaseFields(caseFields);
        //step 5 Invoke IdAM API to get the IdAM Roles of the invoker.
        UserInfo userInfo = getUserInfo();
        //step 6 If the invoker has the role pui-caa then they are allowed to request an NoC for a case in any
        // jurisdiction
        //Else, if they only have a (solicitor) jurisdiction-specific role, then confirm that the jurisdiction of the
        // case matches one of the jurisdictions of the user, error and exit if not.
        validateUserRoles(caseViewResource, userInfo);
        //step 7 Identify the case type Id of the retrieved case
        String caseType = caseViewResource.getCaseType().getName();
        //step 8 n/a
        //step 9 getTabContents - def store
        ChallengeQuestionsResult challengeQuestionsResult =
            definitionStoreRepository.challengeQuestions(caseType, caseId);
        // check if empty and throw error
        //step 10 n/a
        //step 11 For each set of answers in the config, check that there is an OrganisationPolicy
        // field in the case containing the case role, returning an error if this is not true.
        List<OrganisationPolicy> organisationPolicies = findPolicies(caseFields.getCases().stream().findFirst().get());
        checkOrgPoliciesForRoles(challengeQuestionsResult, organisationPolicies);
        return NoCRequestDetails.builder()
            .caseViewResource(caseViewResource)
            .challengeQuestionsResult(challengeQuestionsResult)
            .searchResultViewItem(caseFields.getCases().get(0))
            .build();
    }

    private void checkOrgPoliciesForRoles(ChallengeQuestionsResult challengeQuestionsResult,
                                          List<OrganisationPolicy> organisationPolicies) {
        if (organisationPolicies.isEmpty()) {
            throw new ValidationException("No Org Policy with that role");
        }
        challengeQuestionsResult.getQuestions().forEach(challengeQuestion -> {
            for (ChallengeAnswer answer : challengeQuestion.getAnswers()) {
                String role = answer.getCaseRoleId();
                for (OrganisationPolicy organisationPolicy : organisationPolicies) {
                    if (!organisationPolicy.getOrgPolicyCaseAssignedRole().equals(role)) {
                        throw new ValidationException("No Org Policy with that role");
                    }
                }
            }
        });
    }

    private List<OrganisationPolicy> findPolicies(SearchResultViewItem caseFields) {
        List<JsonNode> policyNodes = caseFields.findOrganisationPolicyNodes();
        return policyNodes.stream()
            .map(node -> OrganisationPolicy.builder()
                .orgPolicyCaseAssignedRole(node.get("OrgPolicyCaseAssignedRole").asText())
                .orgPolicyReference(node.get("OrgPolicyReference").asText()).build())
            .collect(Collectors.toList());
    }

    private Optional<String> extractJurisdiction(String caseworkerRole) {
        String[] parts = caseworkerRole.split("-");
        return parts.length < 2 ? Optional.empty() : Optional.of(parts[JURISDICTION_INDEX]);
    }

    private void validateUserRoles(CaseViewResource caseViewResource, UserInfo userInfo) {
        if (!userInfo.getRoles().contains(PUI_ROLE)) {
            userInfo.getRoles().forEach(role -> {
                Optional<String> jurisdiction = extractJurisdiction(role);
                if (jurisdiction.isPresent()) {
                    if (!caseViewResource.getCaseType().getJurisdiction().getId().equals(jurisdiction.get())) {
                        throw new ValidationException("insufficient privileges");
                    }
                }
            });
        }
    }

    private UserInfo getUserInfo() {
        RequestContext context = RequestContext.getCurrentContext();
        return idamRepository.getUserInfo(context.getRequest().getAuthType());
    }

    private CaseViewResource getCase(String caseId) {
        return dataStoreRepository.findCaseByCaseId(caseId);
    }

    private CaseSearchResultViewResource findCaseBy(String caseTypeId, String caseId) {
        return dataStoreRepository.findCaseBy(caseTypeId, null, caseId);
    }

    private void getCaseFields(CaseSearchResultViewResource caseDetails) {
        Map<String, JsonNode> caseFields = caseDetails.getCases().stream().findFirst().get().getFields();
        List<SearchResultViewHeader> searchResultViewHeaderList =
            caseDetails.getHeaders().stream().findFirst().get().getFields();
        List<SearchResultViewHeader> filteredSearch =
            searchResultViewHeaderList.stream()
                .filter(searchResultViewHeader ->
                            searchResultViewHeader.getCaseFieldTypeDefinition()
                                .getType().equals(CHANGE_ORG_REQUEST)).collect(Collectors.toList());
        for (SearchResultViewHeader searchResultViewHeader : filteredSearch) {
            if (caseFields.containsKey(searchResultViewHeader.getCaseFieldId())) {
                JsonNode node = caseFields.get(searchResultViewHeader.getCaseFieldId());
                if (node.findValues("CaseRoleId") != null) {
                    throw new ValidationException("on going NoC request in progress");
                }
            }
        }
    }

    private void getCaseEvents(CaseViewResource caseViewResource) {
        List<CaseViewEvent> caseViewEventsFiltered =
            Arrays.stream(caseViewResource.getCaseViewEvents())
                .filter(caseViewEvent -> caseViewEvent.getEventName().equalsIgnoreCase(NOC_EVENTS))
                .collect(Collectors.toList());
        if (caseViewEventsFiltered.isEmpty()) {
            throw new ValidationException("no NoC events available for this case type");
        }
    }

    private Optional<OrganisationPolicy> findOrganisationPolicyForRole(SearchResultViewItem caseResult,
                                                                       String caseRoleId) {
        return findPolicies(caseResult).stream()
            .filter(policy -> policy.getOrgPolicyCaseAssignedRole().equals(caseRoleId))
            .findFirst();
    }

    private boolean organisationEqualsRequestingUsers(Organisation organisation) {
        return organisation != null
            && prdRepository.findUsersByOrganisation()
            .getOrganisationIdentifier().equals(organisation.getOrganisationID());
    }
    /**
     * Input parameters to be decided on with Dan, dependant on response from NocAnswers.
     *
     * @param caseId case id
     * @param organisation organisation
     * @param organisationPolicy organisation policy
     * @return RequestNoticeOfChangeResponse
     */
    public RequestNoticeOfChangeResponse requestNoticeOfChange(String caseId,
                                                               Organisation organisation,
                                                               OrganisationPolicy organisationPolicy) {
        // LLD Step 4: Generate the NoCRequest event:
        CaseResource caseResource = generateNoCRequestEvent(caseId, organisation, organisationPolicy);

        // LLD Step 5: Confirm if the NoCRequest has been auto-approved:
        // Reload the case data (EI-3); as the case may have been changed as a result of the post-submit callback to
        // CheckForNoCApproval operation, return error if detected (Response case - 7).
        CaseViewResource caseDetails = getCase(caseId);

        boolean isApprovalComplete =
            isNocRequestAutoApprovalCompleted(caseDetails, organisationPolicy.getOrgPolicyCaseAssignedRole());

        // LLD STep 6: Auto-assign relevant case-roles to the invoker if required, i.e.:
        if (isApprovalComplete  && isActingAsSolicitor()) {
            autoAssignCaseRoles(organisation.getOrganisationID(), caseDetails);
        }

        RequestNoticeOfChangeResponse requestNoticeOfChangeResponse = RequestNoticeOfChangeResponse.builder()
            .caseRole(organisationPolicy.getOrgPolicyCaseAssignedRole())
            .approvalStatus(isApprovalComplete ? APPROVED : PENDING)
            .status(REQUEST_NOTICE_OF_CHANGE_STATUS_MESSAGE)
            .build();

        return requestNoticeOfChangeResponse;
    }

    private boolean isActingAsSolicitor() {
        UserInfo userInfo = getUserInfo();

        // check user info roles as per `validateUserRoles`
        return false;
    }

    private CaseResource generateNoCRequestEvent(String caseId,
                                                 Organisation invokersOrganisation,
                                                 OrganisationPolicy organisationPolicy) {
        ChangeOrganisationRequest changeOrganisationRequest = ChangeOrganisationRequest.builder()
            .caseRoleId(organisationPolicy.getOrgPolicyCaseAssignedRole())
            .organisationToAdd(invokersOrganisation)
            .organisationToRemove(organisationPolicy.getOrganisation())
            .requestTimestamp(LocalDateTime.now())
            .build();

        return dataStoreRepository.submitEventForCase(caseId, NOC_REQUEST, changeOrganisationRequest);

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

    private boolean isNocRequestAutoApprovalCompleted(CaseViewResource caseViewResource, String caseRole) {
        //        Confirm if the following are all true:
        //        ChangeOrganisationRequest.CaseRole is NULL – i.e. the request has been auto-approved and processed.
        //        ChangeOrganisationRequest.ApprovalStatus is NULL - i.e. NULL (approved) or 0 (pending).
        //            The organisation in the OrganisationPolicy identified by the CaseRole saved in Step-2 matches
        //            that of the organisation of the invoker (also found in Step-2) – i.e. the request was to add or
        //            replace representation and it was approved.
        //
        return false;
    }

    private void autoAssignCaseRoles(String organisationId, CaseViewResource caseViewResource) {
        //        From the case data reloaded in Step-5: find the Case Roles in any OrganisationPolicy associated with
        //        the Organisation of the invoker as identified in Step-2.
        //        Make a single call to EI-4 containing a list item for each case-role identified: holding
        //            case_id = as specified in the request
        //        user_id = invoker's IDAM ID
        //        case_role = case role as identified
        //        organisation_id = Organisation of the invoker as identified in Step-2.
        //... return error if detected (Response case - 7).
    }
}
