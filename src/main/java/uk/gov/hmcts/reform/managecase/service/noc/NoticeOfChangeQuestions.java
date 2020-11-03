package uk.gov.hmcts.reform.managecase.service.noc;


import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.ArrayUtils;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseCouldNotBeFetchedException;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.CaseSearchResultViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewHeader;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewHeaderGroup;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_NOT_FOUND;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CHANGE_REQUEST;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.INSUFFICIENT_PRIVILEGE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_EVENT_NOT_AVAILABLE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_REQUEST_ONGOING;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_ORG_POLICY_WITH_ROLE;
import static uk.gov.hmcts.reform.managecase.repository.DefaultDataStoreRepository.CHANGE_ORGANISATION_REQUEST;

@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis",
    "PMD.GodClass",
    "PMD.ExcessiveImports",
    "PMD.TooManyMethods",
    "PMD.AvoidDeeplyNestedIfStmts", "PMD.PreserveStackTrace", "PMD.LawOfDemeter",
    "PMD.AvoidLiteralsInIfCondition", "PMD.CyclomaticComplexity"})
public class NoticeOfChangeQuestions {

    public static final String PUI_ROLE = "pui-caa";


    private static final String CHALLENGE_QUESTION_ID = "NoCChallenge";
    private static final String CASE_ROLE_ID = "CaseRoleId";

    private final DataStoreRepository dataStoreRepository;
    private final DefinitionStoreRepository definitionStoreRepository;

    private final SecurityUtils securityUtils;

    @Autowired
    public NoticeOfChangeQuestions(DataStoreRepository dataStoreRepository,
                                   DefinitionStoreRepository definitionStoreRepository,
                                   SecurityUtils securityUtils) {
        this.dataStoreRepository = dataStoreRepository;
        this.definitionStoreRepository = definitionStoreRepository;
        this.securityUtils = securityUtils;
    }

    public ChallengeQuestionsResult getChallengeQuestions(String caseId) {
        ChallengeQuestionsResult challengeQuestionsResult = challengeQuestions(caseId).getChallengeQuestionsResult();

        List<ChallengeQuestion> challengeQuestionsResponse = challengeQuestionsResult.getQuestions().stream()
            .map(challengeQuestion -> {
                challengeQuestion.setAnswerField(null);
                return challengeQuestion;
            })
            .collect(Collectors.toList());


        return ChallengeQuestionsResult.builder().questions(challengeQuestionsResponse).build();
    }

    public NoCRequestDetails challengeQuestions(String caseId) {
        CaseViewResource caseViewResource = getCase(caseId);
        checkForCaseEvents(caseViewResource);
        CaseSearchResultViewResource caseSearchResultViewResource = findCaseBy(caseViewResource
                                                                                   .getCaseType().getId(), caseId);
        checkCaseFields(caseSearchResultViewResource);
        UserInfo userInfo = getUserInfo();
        validateUserRoles(caseViewResource, userInfo);
        String caseType = caseViewResource.getCaseType().getId();
        ChallengeQuestionsResult challengeQuestionsResult =
            definitionStoreRepository.challengeQuestions(caseType, CHALLENGE_QUESTION_ID);
        Optional<SearchResultViewItem> searchResultViewItem = caseSearchResultViewResource
            .getCases().stream().findFirst();
        if (searchResultViewItem.isPresent()) {
            List<OrganisationPolicy> organisationPolicies = searchResultViewItem.get().findPolicies();
            checkOrgPoliciesForRoles(challengeQuestionsResult, organisationPolicies);
        } else {
            throw new CaseCouldNotBeFetchedException(CASE_NOT_FOUND);
        }

        return NoCRequestDetails.builder()
            .caseViewResource(caseViewResource)
            .challengeQuestionsResult(challengeQuestionsResult)
            .searchResultViewItem(caseSearchResultViewResource.getCases().get(0))
            .build();
    }

    private void checkOrgPoliciesForRoles(ChallengeQuestionsResult challengeQuestionsResult,
                                          List<OrganisationPolicy> organisationPolicies) {
        if (organisationPolicies.isEmpty()) {
            throw new ValidationException(NO_ORG_POLICY_WITH_ROLE);
        }
        challengeQuestionsResult.getQuestions().forEach(challengeQuestion -> {
            boolean missingRole = challengeQuestion.getAnswers().stream()
                .anyMatch(answer -> !isRoleInOrganisationPolicies(organisationPolicies, answer.getCaseRoleId()));
            if (missingRole) {
                throw new ValidationException(NO_ORG_POLICY_WITH_ROLE);
            }
        });
    }

    private boolean isRoleInOrganisationPolicies(List<OrganisationPolicy> organisationPolicies, String role) {
        return organisationPolicies.stream()
            .anyMatch(organisationPolicy -> organisationPolicy.getOrgPolicyCaseAssignedRole().equals(role));
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
        CaseViewResource caseViewResource = new CaseViewResource();
        try {
            caseViewResource = dataStoreRepository.findCaseByCaseId(caseId);
        } catch (FeignException e) {
            if (HttpStatus.NOT_FOUND.value() == e.status()) {
                throw new CaseCouldNotBeFetchedException(CASE_NOT_FOUND);
            } else if (HttpStatus.BAD_REQUEST.value() == e.status()) {
                throw new CaseCouldNotBeFetchedException(CASE_ID_INVALID);
            }
        }
        return caseViewResource;
    }

    private CaseSearchResultViewResource findCaseBy(String caseTypeId, String caseId) {
        return dataStoreRepository.findCaseBy(caseTypeId, null, caseId);
    }

    private void checkCaseFields(CaseSearchResultViewResource caseDetails) {
        if (caseDetails.getCases().isEmpty()) {
            throw new CaseCouldNotBeFetchedException(CASE_NOT_FOUND);
        }
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
                                .equals(CHANGE_ORGANISATION_REQUEST)).collect(toList());
        if (filteredSearch.size() > 1) {
            throw new ValidationException(CHANGE_REQUEST);
        }
        SearchResultViewItem searchResultViewItem = caseDetails.getCases().get(0);
        Map<String, JsonNode> caseFields = searchResultViewItem.getFields();
        for (SearchResultViewHeader searchResultViewHeader : filteredSearch) {
            if (caseFields.containsKey(searchResultViewHeader.getCaseFieldId())) {
                JsonNode node = caseFields.get(searchResultViewHeader.getCaseFieldId());
                if (node != null) {
                    JsonNode caseRoleId = node.findPath(CASE_ROLE_ID);
                    if (!caseRoleId.isNull()) {
                        throw new ValidationException(NOC_REQUEST_ONGOING);
                    }
                }
            }
        }
    }

    private void checkForCaseEvents(CaseViewResource caseViewResource) {
        if (caseViewResource.getCaseViewActionableEvents() == null
            || ArrayUtils.isEmpty(caseViewResource.getCaseViewActionableEvents())) {
            throw new ValidationException(NOC_EVENT_NOT_AVAILABLE);
        }
    }
}
