package uk.gov.hmcts.reform.managecase.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDataContent;
import uk.gov.hmcts.reform.managecase.client.datastore.Event;
import uk.gov.hmcts.reform.managecase.client.datastore.StartEventResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewActionableEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;

import javax.validation.ValidationException;

import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.EVENT_TOKEN_NOT_PRESENT;

@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.GodClass", "PMD.ExcessiveImports", "PMD.TooManyMethods"})
public class NoticeOfChangeApprovalService {

    private static final int EXPECTED_NUMBER_OF_EVENTS = 1;
    private static final String NOC_DECISION_EVENT_UNIDENTIFIABLE = "NoC Decision event could not be identified";
    static final String CHECK_NOC_APPROVAL_DESCRIPTION = "Check Notice of Change Approval Event";

    private final DataStoreRepository dataStoreRepository;

    @Autowired
    public NoticeOfChangeApprovalService(
        @Qualifier("nocApprovalDataStoreRepository") DataStoreRepository dataStoreRepository) {
        this.dataStoreRepository = dataStoreRepository;
    }

    public void checkNoticeOfChangeApproval(String caseId) {
        CaseViewResource caseViewResource = dataStoreRepository.findCaseByCaseId(caseId);
        CaseViewActionableEvent[] events = caseViewResource.getCaseViewActionableEvents();


        if (events.length != EXPECTED_NUMBER_OF_EVENTS) {
            throw new ValidationException(NOC_DECISION_EVENT_UNIDENTIFIABLE);
        }

        String eventId = events[0].getId();

        StartEventResource startEvent = dataStoreRepository.getExternalStartEventTrigger(caseId, eventId);

        if (startEvent != null) {
            if (startEvent.getToken() == null) {
                throw new ValidationException(EVENT_TOKEN_NOT_PRESENT);
            }

            CaseDataContent caseDataContent = CaseDataContent.builder()
                .token(startEvent.getToken())
                .event(Event.builder()
                    .eventId(eventId)
                    .description(CHECK_NOC_APPROVAL_DESCRIPTION)
                    .build())
                .data(startEvent.getCaseDetails().getData())
                .build();

            dataStoreRepository.submitEventForCaseOnly(caseId, caseDataContent);
        }
    }
}
