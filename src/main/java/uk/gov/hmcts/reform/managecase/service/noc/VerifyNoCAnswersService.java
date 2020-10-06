package uk.gov.hmcts.reform.managecase.service.noc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.payload.VerifyNoCAnswersRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.model.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.service.NoticeOfChangeService;

import javax.validation.ValidationException;

@Service
public class VerifyNoCAnswersService {

    private final NoticeOfChangeService noticeOfChangeService;
    private final ChallengeAnswerValidator challengeAnswerValidator;
    private final PrdRepository prdRepository;

    @Autowired
    public VerifyNoCAnswersService(NoticeOfChangeService noticeOfChangeService,
                                   ChallengeAnswerValidator challengeAnswerValidator,
                                   PrdRepository prdRepository) {
        this.noticeOfChangeService = noticeOfChangeService;
        this.challengeAnswerValidator = challengeAnswerValidator;
        this.prdRepository = prdRepository;
    }

    public NoCRequestDetails verifyNoCAnswers(VerifyNoCAnswersRequest verifyNoCAnswersRequest) {
        String caseId = verifyNoCAnswersRequest.getCaseId();
        NoCRequestDetails noCRequestDetails = noticeOfChangeService.challengeQuestions(caseId);
        SearchResultViewItem caseResult = noCRequestDetails.getSearchResultViewItem();

        String caseRoleId = challengeAnswerValidator
            .getMatchingCaseRole(noCRequestDetails.getChallengeQuestionsResult(),
                verifyNoCAnswersRequest.getAnswers(), caseResult);

        OrganisationPolicy organisationPolicy = caseResult.findOrganisationPolicyForRole(caseRoleId)
            .orElseThrow(() -> new ValidationException(String.format(
                "No OrganisationPolicy exists on the case for the case role '%s'", caseRoleId)));

        if (organisationEqualsRequestingUsers(organisationPolicy.getOrganisation())) {
            throw new ValidationException("The requestor has answered questions uniquely identifying"
                + " a litigant that they are already representing");
        }

        noCRequestDetails.setOrganisationPolicy(organisationPolicy);
        return noCRequestDetails;
    }

    private boolean organisationEqualsRequestingUsers(Organisation organisation) {
        return organisation != null
            && prdRepository.findUsersByOrganisation()
            .getOrganisationIdentifier().equals(organisation.getOrganisationID());
    }
}
