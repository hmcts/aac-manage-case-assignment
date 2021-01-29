package uk.gov.hmcts.reform.managecase.service.noc;


import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.ArrayUtils;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseCouldNotBeFoundException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCException;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestion;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;


import java.util.List;

import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.CASE_ID_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_NOT_FOUND;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.CHANGE_REQUEST;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.INSUFFICIENT_PRIVILEGE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.MULTIPLE_NOC_REQUEST_EVENTS;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.NOC_EVENT_NOT_AVAILABLE;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.NOC_REQUEST_ONGOING;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.NO_ORG_POLICY_WITH_ROLE;

@Service
public class NoticeOfChangeQuestions {

    public static final String PUI_ROLE = "pui-caa";

    private static final String CHALLENGE_QUESTION_ID = "NoCChallenge";

    private final DataStoreRepository dataStoreRepository;
    private final DefinitionStoreRepository definitionStoreRepository;
    private final PrdRepository prdRepository;
    private final JacksonUtils jacksonUtils;
    private final SecurityUtils securityUtils;

    @Autowired
    public NoticeOfChangeQuestions(@Qualifier("defaultDataStoreRepository") DataStoreRepository dataStoreRepository,
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

    public ChallengeQuestionsResult getChallengeQuestions(String caseId) {
        ChallengeQuestionsResult challengeQuestionsResult = challengeQuestions(caseId).getChallengeQuestionsResult();

        List<ChallengeQuestion> challengeQuestionsResponse = challengeQuestionsResult.getQuestions().stream()
            .map(challengeQuestion -> {
                challengeQuestion.setAnswerField(null);
                return challengeQuestion;
            })
            .collect(toList());


        return ChallengeQuestionsResult.builder().questions(challengeQuestionsResponse).build();
    }

    public NoCRequestDetails challengeQuestions(String caseId) {
        CaseViewResource caseViewResource = getCase(caseId);
        checkForCaseEvents(caseViewResource);

        CaseDetails caseDetails = dataStoreRepository.findCaseByCaseIdExternalApi(caseId);
        checkCaseFields(caseDetails);
        validateUserRoles(getUserInfo());
        ChallengeQuestionsResult challengeQuestionsResult =
            definitionStoreRepository.challengeQuestions(caseDetails.getCaseTypeId(), CHALLENGE_QUESTION_ID);

        List<OrganisationPolicy> organisationPolicies = findPolicies(caseDetails);
        checkOrgPoliciesForRoles(challengeQuestionsResult, organisationPolicies);

        return NoCRequestDetails.builder()
            .caseViewResource(caseViewResource)
            .challengeQuestionsResult(challengeQuestionsResult)
            .caseDetails(caseDetails)
            .build();
    }

    private void checkOrgPoliciesForRoles(ChallengeQuestionsResult challengeQuestionsResult,
                                          List<OrganisationPolicy> organisationPolicies) {
        if (organisationPolicies.isEmpty()) {
            throw new NoCException(NO_ORG_POLICY_WITH_ROLE);
        }
        challengeQuestionsResult.getQuestions().forEach(challengeQuestion -> {
            boolean missingRole = challengeQuestion.getAnswers().stream()
                .anyMatch(answer -> !isRoleInOrganisationPolicies(organisationPolicies, answer.getCaseRoleId()));
            if (missingRole) {
                throw new NoCException(NO_ORG_POLICY_WITH_ROLE);
            }
        });
    }

    private boolean isRoleInOrganisationPolicies(List<OrganisationPolicy> organisationPolicies, String role) {
        return organisationPolicies.stream()
            .anyMatch(organisationPolicy -> organisationPolicy.getOrgPolicyCaseAssignedRole().equals(role));
    }

    private void validateUserRoles(UserInfo userInfo) {
        List<String> roles = userInfo.getRoles();
        if (!roles.contains(PUI_ROLE)
            && !isActingAsSolicitor(roles)) {
            throw new NoCException(INSUFFICIENT_PRIVILEGE);
        }
    }

    private boolean isActingAsSolicitor(List<String> roles) {
        return securityUtils.hasSolicitorRole(roles);
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
                throw new CaseCouldNotBeFoundException(CASE_NOT_FOUND);
            } else if (HttpStatus.BAD_REQUEST.value() == e.status()) {
                throw new NoCException(CASE_ID_INVALID);
            }
        }
        return caseViewResource;
    }

    private void checkCaseFields(CaseDetails caseDetails) {

        if (caseDetails.findCorNodes().size() > 1) {
            throw new NoCException(CHANGE_REQUEST);
        }

        if (caseDetails.hasCaseRoleId()) {
            throw new NoCException(NOC_REQUEST_ONGOING);
        }
    }

    private void checkForCaseEvents(CaseViewResource caseViewResource) {
        if (caseViewResource.getCaseViewActionableEvents() == null
            || ArrayUtils.isEmpty(caseViewResource.getCaseViewActionableEvents())) {
            throw new NoCException(NOC_EVENT_NOT_AVAILABLE);
        } else if (caseViewResource.getCaseViewActionableEvents().length != 1) {
            throw new NoCException(MULTIPLE_NOC_REQUEST_EVENTS);
        }
    }

    private List<OrganisationPolicy> findPolicies(CaseDetails caseDetails) {
        List<JsonNode> policyNodes = caseDetails.findOrganisationPolicyNodes();
        return policyNodes.stream()
            .map(node -> jacksonUtils.convertValue(node, OrganisationPolicy.class))
            .collect(toList());
    }
}
