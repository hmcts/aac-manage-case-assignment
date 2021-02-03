package uk.gov.hmcts.reform.managecase.service.noc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCException;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import java.util.Optional;

import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.REQUESTOR_ALREADY_REPRESENTS;

@Service
public class VerifyNoCAnswersService {

    private final NoticeOfChangeQuestions noticeOfChangeQuestions;
    private final ChallengeAnswerValidator challengeAnswerValidator;
    private final PrdRepository prdRepository;
    private final JacksonUtils jacksonUtils;

    @Autowired
    public VerifyNoCAnswersService(NoticeOfChangeQuestions noticeOfChangeQuestions,
                                   ChallengeAnswerValidator challengeAnswerValidator,
                                   PrdRepository prdRepository, JacksonUtils jacksonUtils) {
        this.noticeOfChangeQuestions = noticeOfChangeQuestions;
        this.challengeAnswerValidator = challengeAnswerValidator;
        this.prdRepository = prdRepository;
        this.jacksonUtils = jacksonUtils;
    }

    public NoCRequestDetails verifyNoCAnswers(VerifyNoCAnswersRequest verifyNoCAnswersRequest) {
        String caseId = verifyNoCAnswersRequest.getCaseId();
        NoCRequestDetails noCRequestDetails = noticeOfChangeQuestions.challengeQuestions(caseId);
        CaseDetails caseDetails = noCRequestDetails.getCaseDetails();

        String caseRoleId = challengeAnswerValidator
            .getMatchingCaseRole(noCRequestDetails.getChallengeQuestionsResult(),
                verifyNoCAnswersRequest.getAnswers(), caseDetails);

        OrganisationPolicy organisationPolicy = findPolicy(caseDetails, caseRoleId)
            .orElseThrow(() -> new NoCException((String.format("No OrganisationPolicy exists on the case for "
                                                                   + "the case role '%s'",
                                                               caseRoleId)), "no-org-policy"));

        if (organisationEqualsRequestingUsers(organisationPolicy.getOrganisation())) {
            throw new NoCException(REQUESTOR_ALREADY_REPRESENTS);
        }

        noCRequestDetails.setOrganisationPolicy(organisationPolicy);
        return noCRequestDetails;
    }

    private boolean organisationEqualsRequestingUsers(Organisation organisation) {
        return organisation != null
            && prdRepository.findUsersByOrganisation()
            .getOrganisationIdentifier().equals(organisation.getOrganisationID());
    }

    private Optional<OrganisationPolicy> findPolicy(CaseDetails caseDetails, String caseRoleId) {
        return caseDetails.findOrganisationPolicyNodes().stream()
            .map(node -> jacksonUtils.convertValue(node, OrganisationPolicy.class))
            .filter(policy -> caseRoleId.equalsIgnoreCase(policy.getOrgPolicyCaseAssignedRole()))
            .findFirst();
    }
}
