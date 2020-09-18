package uk.gov.hmcts.reform.managecase.service;


import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewField;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.ChallengeQuestionsResult;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;

import javax.validation.ValidationException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoticeOfChangeService {

    public static final String SOLICITOR_ROLE = "caseworker-%s-solicitor";
    public static final String PUI_ROLE = "pui-caa";
    public static final String NOC_EVENTS = "NOC";
    public static final String CHANGE_ORG_REQUEST = "ChangeOrganisationRequest";


    private final DataStoreRepository dataStoreRepository;
    private final DefinitionStoreRepository definitionStoreRepository;
    private final IdamRepository idamRepository;

    @Autowired
    public NoticeOfChangeService(DataStoreRepository dataStoreRepository,
                                 IdamRepository idamRepository,
                                 DefinitionStoreRepository definitionStoreRepository) {
        this.dataStoreRepository = dataStoreRepository;
        this.idamRepository = idamRepository;
        this.definitionStoreRepository = definitionStoreRepository;
    }

    public ChallengeQuestionsResult getChallengeQuestions(String caseId) {
        ChallengeQuestionsResult challengeQuestionsResult = challengeQuestions(caseId);
        challengeQuestionsResult.getQuestions().forEach(challengeQuestion -> challengeQuestion.setAnswerField(null));
        return challengeQuestionsResult;
    }

    public ChallengeQuestionsResult challengeQuestions(String caseId) {
        //step 2 getCaseUsingGET(case Id) return error if case # invalid/not found
        CaseViewResource caseViewResource = getCase(caseId);
        //step 3 Check to see what events are available on the case (the system user with IdAM Role caseworker-caa only has access to NoC events).  If no events are available, return an error
        getCaseEvents(caseViewResource);
        //step 4 Check the ChangeOrganisationRequest.CaseRole in the case record.  If it is not null, return an error indicating that there is an ongoing NoCRequest.
        getCaseFields(caseViewResource);
        //step 5 Invoke IdAM API to get the IdAM Roles of the invoker.
        UserInfo userInfo = getUserInfo();
        //step 6 If the invoker has the role pui-caa then they are allowed to request an NoC for a case in any jurisdiction
        //Else, if they only have a (solicitor) jurisdiction-specific role, then confirm that the jurisdiction of the case matches one of the jurisdictions of the user, error and exit if not.
        validateUserRoles(caseViewResource, userInfo);
        //step 7 Identify the case type Id of the retrieved case
        String caseType = caseViewResource.getCaseType().getName();
        //step 8 n/a
        //step 9 getTabContents - def store
        ChallengeQuestionsResult challengeQuestionsResult = definitionStoreRepository.challengeQuestions(caseType, caseId);
        //step 10 strip out "NoCChallenge"

        //step 11 For each set of answers in the config, check that there is an OrganisationPolicy field in the case containing the case role, returning an error if this is not true.

        return challengeQuestionsResult;
    }

    private void validateUserRoles(CaseViewResource caseViewResource, UserInfo userInfo) {
        if (userInfo.getRoles().contains(SOLICITOR_ROLE) || userInfo.getRoles().contains(PUI_ROLE)) {
            userInfo.getRoles().forEach(role -> {
                //TODO tidy up regex
                if (role.matches(SOLICITOR_ROLE)) {
                    role.replace("caseworker-", "");
                    role.replace("-solicitor", "");
                    if (!caseViewResource.getCaseType().getJurisdiction().getName().equals(role)) {
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

    private void getCaseFields(CaseViewResource caseViewResource) {
        List<CaseViewField> caseViewFields = caseViewResource.getMetadataFields().stream()
            .filter(caseViewField -> caseViewField.getFieldTypeDefinition().getType().equals(CHANGE_ORG_REQUEST)).collect(Collectors.toList());
        caseViewFields.forEach(caseViewField -> {
            if (caseViewField.getId().equals("CaseRoleId")) {
                if (caseViewField.getValue() != null) {
                    throw new ValidationException("on going NoC request in progress");
                }
            }
        });
    }

    private void getCaseEvents(CaseViewResource caseViewResource) {
        List<CaseViewEvent> caseViewEventsFiltered = Arrays.stream(caseViewResource.getCaseViewEvents()).filter(caseViewEvent -> caseViewEvent.getEventName().equalsIgnoreCase(NOC_EVENTS)).collect(Collectors.toList());
        if (caseViewEventsFiltered.isEmpty()) {
            throw new ValidationException("no NoC events available for this case type");
        }
    }

}
