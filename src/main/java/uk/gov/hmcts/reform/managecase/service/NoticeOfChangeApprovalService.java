package uk.gov.hmcts.reform.managecase.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDataContent;
import uk.gov.hmcts.reform.managecase.client.datastore.Event;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseUpdateViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewField;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import javax.validation.ValidationException;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.EVENT_TOKEN_NOT_PRESENT;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST;
import static uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition.PREDEFINED_COMPLEX_ORGANISATION_POLICY;

@Service
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.GodClass", "PMD.ExcessiveImports", "PMD.TooManyMethods"})
public class NoticeOfChangeApprovalService {

    private static final int EXPECTED_NUMBER_OF_EVENTS = 1;
    private static final String NOC_DECISION_EVENT_UNIDENTIFIABLE = "NoC Decision event could not be identified";
    static final String CHECK_NOC_APPROVAL_DESCRIPTION = "Check Notice of Change Approval Event";

    private final DataStoreRepository dataStoreRepository;
    private final JacksonUtils jacksonUtils;

    @Autowired
    public NoticeOfChangeApprovalService(
        @Qualifier("nocApprovalDataStoreRepository") DataStoreRepository dataStoreRepository,
        JacksonUtils jacksonUtils) {
        this.dataStoreRepository = dataStoreRepository;
        this.jacksonUtils = jacksonUtils;
    }

    public void checkNoticeOfChangeApproval(String caseId) {
        CaseViewResource caseViewResource = dataStoreRepository.findCaseByCaseId(caseId);
        CaseViewEvent[] events = caseViewResource.getCaseViewEvents();


        if (events.length != EXPECTED_NUMBER_OF_EVENTS) {
            throw new ValidationException(NOC_DECISION_EVENT_UNIDENTIFIABLE);
        }

        String eventId = events[0].getEventId();

        CaseUpdateViewEvent caseUpdateViewEvent = dataStoreRepository.getStartEventTrigger(caseId, eventId);

        if (caseUpdateViewEvent != null) {
            if (caseUpdateViewEvent.getEventToken() == null) {
                throw new ValidationException(EVENT_TOKEN_NOT_PRESENT);
            }

            Map<String, JsonNode> data =
                caseUpdateViewEvent.getCaseFields().stream()
                    .filter(cvf -> cvf.getFieldTypeDefinition().getId().equals(PREDEFINED_COMPLEX_ORGANISATION_POLICY)
                        || cvf.getFieldTypeDefinition().getId().equals(PREDEFINED_COMPLEX_CHANGE_ORGANISATION_REQUEST))
                    .collect(Collectors.toMap(
                        CaseViewField::getId, cvf -> jacksonUtils.convertValue(cvf, JsonNode.class)));


            Event event = Event.builder()
                .eventId(eventId)
                .description(CHECK_NOC_APPROVAL_DESCRIPTION)
                .build();

            CaseDataContent caseDataContent = CaseDataContent.builder()
                .token(caseUpdateViewEvent.getEventToken())
                .event(event)
                .data(data)
                .build();
            dataStoreRepository.submitEventForCaseOnly(caseId, caseDataContent);
        }
    }
}
