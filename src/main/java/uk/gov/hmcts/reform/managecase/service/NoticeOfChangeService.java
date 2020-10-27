package uk.gov.hmcts.reform.managecase.service;


import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseCouldNotBeFetchedException;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewHeader;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewHeaderGroup;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeAnswer;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.FieldType;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.INSUFFICIENT_PRIVILEGE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_EVENT_NOT_AVAILABLE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_REQUEST_ONGOING;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_ORG_POLICY_WITH_ROLE;

@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.ExcessiveImports",
    "PMD.AvoidDeeplyNestedIfStmts", "PMD.PreserveStackTrace", "PMD.LawOfDemeter"})
public class NoticeOfChangeService {

    public static final String PUI_ROLE = "pui-caa";
    public static final String CHANGE_ORG_REQUEST = "ChangeOrganisationRequest";
    private static final String CHALLENGE_QUESTION_ID = "NoCChallenge";
    private static final String CASE_ROLE_ID = "CaseRoleId";

    private final DataStoreRepository dataStoreRepository;
    private final DefinitionStoreRepository definitionStoreRepository;
    private final SecurityUtils securityUtils;

    @Autowired
    public NoticeOfChangeService(DataStoreRepository dataStoreRepository,
                                 DefinitionStoreRepository definitionStoreRepository,
                                 SecurityUtils securityUtils) {

        this.dataStoreRepository = dataStoreRepository;
        this.definitionStoreRepository = definitionStoreRepository;
        this.securityUtils = securityUtils;
    }

    public ChallengeQuestionsResult getChallengeQuestions(String caseId) {
        ChallengeQuestionsResult challengeQuestionsResult = challengeQuestions(caseId).getChallengeQuestionsResult();
        //Step 12 Remove the answer section from the JSON returned byGetTabContents and return success with the
        // remaining JSOn

        List<ChallengeQuestion> challengeQuestionsResponse = challengeQuestionsResult.getQuestions().stream()
            .map(challengeQuestion -> ChallengeQuestion.builder()
                .questionText(challengeQuestion.getQuestionText())
                .caseTypeId(challengeQuestion.getCaseTypeId())
                .order(challengeQuestion.getOrder())
                .answerFieldType(FieldType.builder()
                                     .collectionFieldType(challengeQuestion.getAnswerFieldType()
                                                              .getCollectionFieldType())
                                     .complexFields(challengeQuestion.getAnswerFieldType()
                                                        .getComplexFields())
                                     .fixedListItems(challengeQuestion.getAnswerFieldType()
                                                         .getFixedListItems())
                                     .regularExpression(challengeQuestion.getAnswerFieldType()
                                                            .getRegularExpression())
                                     .max(challengeQuestion.getAnswerFieldType().getMax())
                                     .min(challengeQuestion.getAnswerFieldType().getMin())
                                     .id(challengeQuestion.getAnswerFieldType().getId())
                                     .type(challengeQuestion.getAnswerFieldType().getType())
                                     .build())
                .displayContextParameter(challengeQuestion.getDisplayContextParameter())
                .challengeQuestionId(challengeQuestion.getChallengeQuestionId())
                .build())
            .collect(Collectors.toList());


        return ChallengeQuestionsResult.builder().questions(challengeQuestionsResponse).build();
    }

    public NoCRequestDetails challengeQuestions(String caseId) {
        //step 2 getCaseUsingGET(case Id) return error if case # invalid/not found
        CaseViewResource caseViewResource = getCase(caseId);
        //step 3 Check to see what events are available on the case (the system user with IdAM Role caseworker-caa only
        // has access to NoC events).  If no events are available, return an error
        checkForCaseEvents(caseViewResource);
        //step 4 Check the ChangeOrganisationRequest.CaseRole in the case record.  If it is not null, return an error
        // indicating that there is an ongoing NoCRequest.
        CaseSearchResultViewResource caseFields = findCaseBy(caseViewResource.getCaseType().getId(), caseId);
        checkCaseFields(caseFields);
        //step 5 Invoke IdAM API to get the IdAM Roles of the invoker.
        UserInfo userInfo = getUserInfo();
        //step 6 If the invoker has the role pui-caa then they are allowed to request an NoC for a case in any
        // jurisdiction
        //Else, if they only have a (solicitor) jurisdiction-specific role, then confirm that the jurisdiction of the
        // case matches one of the jurisdictions of the user, error and exit if not.
        validateUserRoles(caseViewResource, userInfo);
        //step 7 Identify the case type Id of the retrieved case
        String caseType = caseViewResource.getCaseType().getId();
        //step 8 n/a
        //step 9 getTabContents - def store
        ChallengeQuestionsResult challengeQuestionsResult =
            definitionStoreRepository.challengeQuestions(caseType, CHALLENGE_QUESTION_ID);
        // check if empty and throw error
        //step 10 n/a
        //step 11 For each set of answers in the config, check that there is an OrganisationPolicy
        // field in the case containing the case role, returning an error if this is not true.
        Optional<SearchResultViewItem> searchResultViewItem = caseFields.getCases().stream().findFirst();
        if (searchResultViewItem.isPresent()) {
            List<OrganisationPolicy> organisationPolicies = searchResultViewItem.get().findPolicies();
            checkOrgPoliciesForRoles(challengeQuestionsResult, organisationPolicies);
        }

        return NoCRequestDetails.builder()
            .caseViewResource(caseViewResource)
            .challengeQuestionsResult(challengeQuestionsResult)
            .searchResultViewItem(caseFields.getCases().get(0))
            .build();
    }

    private void checkOrgPoliciesForRoles(ChallengeQuestionsResult challengeQuestionsResult,
                                          List<OrganisationPolicy> organisationPolicies) {
        if (organisationPolicies.isEmpty()) {
            throw new ValidationException(NO_ORG_POLICY_WITH_ROLE);
        }
        challengeQuestionsResult.getQuestions().forEach(challengeQuestion -> {
            for (ChallengeAnswer answer : challengeQuestion.getAnswers()) {
                String role = answer.getCaseRoleId();
                if (!isRoleInOrganisationPolicies(organisationPolicies, role)) {
                    throw new ValidationException(NO_ORG_POLICY_WITH_ROLE);
                }
            }
        });
    }

    private boolean isRoleInOrganisationPolicies(List<OrganisationPolicy> organisationPolicies, String role) {
        boolean roleFound = false;
        for (OrganisationPolicy organisationPolicy : organisationPolicies) {
            if (organisationPolicy.getOrgPolicyCaseAssignedRole().equals(role)) {
                roleFound = true;
            }
        }
        return roleFound;
    }

    private void validateUserRoles(CaseViewResource caseViewResource, UserInfo userInfo) {
        List<String> roles = userInfo.getRoles();
        if (!roles.contains(PUI_ROLE)
            && !isActingAsSolicitor(roles, caseViewResource.getCaseType().getJurisdiction().getId())) {
            throw new ValidationException(INSUFFICIENT_PRIVILEGE);
        }
    }

    private boolean isActingAsSolicitor(List<String> roles, String jurisdiction) {
        return securityUtils.hasSolicitorRole(roles, jurisdiction);
    }

    private UserInfo getUserInfo() {
        return securityUtils.getUserInfo();
    }

    private CaseViewResource getCase(String caseId) {
        CaseViewResource caseViewResource = null;
        try {
            caseViewResource = dataStoreRepository.findCaseByCaseId(caseId);
        } catch (RestClientResponseException e) {
            if (HttpStatus.NOT_FOUND.value() == e.getRawStatusCode()) {
                throw new CaseCouldNotBeFetchedException("Case could not be found");
            }
        }
        return caseViewResource;
    }

    private CaseSearchResultViewResource findCaseBy(String caseTypeId, String caseId) {
        return dataStoreRepository.findCaseBy(caseTypeId, null, caseId);
    }

    private void checkCaseFields(CaseSearchResultViewResource caseDetails) {
        if (caseDetails.getCases().isEmpty()) {
            throw new CaseCouldNotBeFetchedException("Case could not be found");
        }
        Optional<SearchResultViewItem> searchResultViewItem = caseDetails.getCases().stream().findFirst();
        if (searchResultViewItem.isPresent()) {
            Map<String, JsonNode> caseFields = searchResultViewItem.get().getFields();
            List<SearchResultViewHeader> searchResultViewHeaderList = new ArrayList<>();
            Optional<SearchResultViewHeaderGroup> searchResultViewHeaderGroup =
                caseDetails.getHeaders().stream().findFirst();
            if (searchResultViewHeaderGroup.isPresent()) {
                searchResultViewHeaderList = searchResultViewHeaderGroup.get().getFields();
            }
            List<SearchResultViewHeader> filteredSearch =
                searchResultViewHeaderList.stream()
                    .filter(searchResultViewHeader ->
                                searchResultViewHeader.getCaseFieldTypeDefinition().getId()
                                    .equals(CHANGE_ORG_REQUEST)).collect(Collectors.toList());
            for (SearchResultViewHeader searchResultViewHeader : filteredSearch) {
                if (caseFields.containsKey(searchResultViewHeader.getCaseFieldId())) {
                    JsonNode node = caseFields.get(searchResultViewHeader.getCaseFieldId());
                    if (node.findValues(CASE_ROLE_ID) != null) {
                        throw new ValidationException(NOC_REQUEST_ONGOING);
                    }
                }
            }
        }
    }

    private void checkForCaseEvents(CaseViewResource caseViewResource) {
        if (caseViewResource.getCaseViewActionableEvents() == null) {
            throw new ValidationException(NOC_EVENT_NOT_AVAILABLE);
        }
    }

}
