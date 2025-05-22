package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseEventCreationPayload;
import uk.gov.hmcts.reform.managecase.client.datastore.StartEventResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewActionableEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.repository.NocApprovalDataStoreRepository;

import jakarta.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_DECISION_EVENT_UNIDENTIFIABLE;

public class NoticeOfChangeApprovalServiceTest {

    private static final String CASE_ID = "1567934206391385";


    @InjectMocks
    private NoticeOfChangeApprovalService service;

    @Mock
    private NocApprovalDataStoreRepository repository;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Nested
    @DisplayName("Check Notice of Change Approval")
    class CheckNoticeOfChangeApproval {

        public static final String EVENT_ID = "NOC";
        public static final String EVENT_TOKEN = "eventToken";

        private CaseViewActionableEvent caseViewEvent;
        private CaseViewResource caseViewResource;
        private StartEventResource startEventResource;
        private Map<String, JsonNode> data;

        @BeforeEach
        void setUp() {
            data = new HashMap<>();

            startEventResource = StartEventResource.builder()
                .eventId(EVENT_ID)
                .token(EVENT_TOKEN)
                .caseDetails(CaseDetails.builder().id(CASE_ID).data(data).build())
                .build();

            caseViewEvent = new CaseViewActionableEvent();
            caseViewEvent.setId(EVENT_ID);
            caseViewResource = new CaseViewResource();
            caseViewResource.setCaseViewActionableEvents(new CaseViewActionableEvent[]{caseViewEvent});

            given(repository.findCaseByCaseId(CASE_ID))
                .willReturn(caseViewResource);
            given(repository.getExternalStartEventTrigger(anyString(), anyString()))
                .willReturn(startEventResource);
        }

        @Test
        @DisplayName("Submit Event for Check NoC Approval")
        void checkNoticeOfChangeApproval() {
            service.findAndTriggerNocDecisionEvent(CASE_ID);

            ArgumentCaptor<CaseEventCreationPayload> captor = ArgumentCaptor.forClass(CaseEventCreationPayload.class);
            verify(repository).submitEventForCase(eq(CASE_ID), captor.capture());

            assertThat(captor.getValue().getEvent().getEventId()).isEqualTo(EVENT_ID);
            assertThat(captor.getValue().getToken()).isEqualTo(EVENT_TOKEN);
            assertThat(captor.getValue().getData()).isEqualTo(data);
        }

        @Test
        @DisplayName("must return an error when the startEventResource token is null")
        void shouldThrowErrorWhenCaseUpdateViewEventTokenIsNull() {
            startEventResource = StartEventResource.builder()
                .eventId("NOC")
                .build();

            given(repository.getExternalStartEventTrigger(anyString(), anyString()))
                .willReturn(startEventResource);

            assertThatThrownBy(() -> service.findAndTriggerNocDecisionEvent(CASE_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Event token not present");
        }

        @Test
        @DisplayName("must return an error when the CaseViewActionableEvent list is empty")
        void shouldThrowErrorWhenCaseViewEventListIsEmpty() {
            caseViewResource.setCaseViewActionableEvents(new CaseViewActionableEvent[]{});

            assertThatThrownBy(() -> service.findAndTriggerNocDecisionEvent(CASE_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(NOC_DECISION_EVENT_UNIDENTIFIABLE);
        }

        @Test
        @DisplayName("must return an error when the CaseViewActionableEvent list length is greater than one")
        void shouldThrowErrorWhenCaseViewEventListLengthGreaterThanOne() {
            caseViewResource.setCaseViewActionableEvents(new CaseViewActionableEvent[]{caseViewEvent, caseViewEvent});

            assertThatThrownBy(() -> service.findAndTriggerNocDecisionEvent(CASE_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(NOC_DECISION_EVENT_UNIDENTIFIABLE);
        }
    }
}
