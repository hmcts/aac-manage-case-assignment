package uk.gov.hmcts.reform.managecase.service.noc;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseCouldNotBeFoundException;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseEventCreationPayload;
import uk.gov.hmcts.reform.managecase.client.datastore.Event;
import uk.gov.hmcts.reform.managecase.client.datastore.StartEventResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewActionableEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;

import jakarta.validation.ValidationException;

import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_NOT_FOUND;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.EVENT_TOKEN_NOT_PRESENT;

@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.GodClass", "PMD.ExcessiveImports", "PMD.TooManyMethods"})
public class NoticeOfChangeApprovalService {

    private static final int EXPECTED_NUMBER_OF_EVENTS = 1;
    private static final String NOC_DECISION_EVENT_UNIDENTIFIABLE = "NoC Decision event could not be identified";
    public static final String APPLY_NOC_DECISION_EVENT = "Apply NocDecision Event";

    private final DataStoreRepository dataStoreRepository;

    @Autowired
    public NoticeOfChangeApprovalService(
        @Qualifier("nocApprovalDataStoreRepository") DataStoreRepository dataStoreRepository) {
        this.dataStoreRepository = dataStoreRepository;
    }

    public void findAndTriggerNocDecisionEvent(String caseId) {
        CaseViewResource caseViewResource = findCaseByCaseId(caseId);
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

            CaseEventCreationPayload caseEventCreationPayload = CaseEventCreationPayload.builder()
                .token(startEvent.getToken())
                .event(Event.builder()
                    .eventId(eventId)
                    .summary(APPLY_NOC_DECISION_EVENT)
                    .description(APPLY_NOC_DECISION_EVENT)
                    .build())
                .data(startEvent.getCaseDetails().getData())
                .build();

            dataStoreRepository.submitEventForCase(caseId, caseEventCreationPayload);
        }
    }

    private CaseViewResource findCaseByCaseId(String caseId) {
        CaseViewResource caseViewResource;
        try {
            caseViewResource = dataStoreRepository.findCaseByCaseId(caseId);
        } catch (FeignException e) {
            if (HttpStatus.NOT_FOUND.value() == e.status()) {
                throw new CaseCouldNotBeFoundException(CASE_NOT_FOUND);
            }
            throw e;
        }
        return caseViewResource;
    }
}
