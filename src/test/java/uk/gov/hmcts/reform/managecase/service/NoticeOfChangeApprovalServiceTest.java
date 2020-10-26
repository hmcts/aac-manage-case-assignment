package uk.gov.hmcts.reform.managecase.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDataContent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseUpdateViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewField;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition;
import uk.gov.hmcts.reform.managecase.repository.NocApprovalDataStoreRepository;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_DECISION_EVENT_UNIDENTIFIABLE;

public class NoticeOfChangeApprovalServiceTest {

    private static final String CASE_ID = "1567934206391385";


    @InjectMocks
    private NoticeOfChangeApprovalService service;

    @Mock
    private NocApprovalDataStoreRepository repository;
    @Mock
    private JacksonUtils jacksonUtils;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Nested
    @DisplayName("Check Notice of Change Approval")
    class CheckNoticeOfChangeApproval {

        private CaseViewEvent caseViewEvent;
        private CaseViewResource caseViewResource;
        private CaseUpdateViewEvent caseUpdateViewEvent;
        private CaseViewField caseViewField;
        private FieldTypeDefinition fieldTypeDefinition;
        private JsonNode node;

        @BeforeEach
        void setUp() {
            node = mock(JsonNode.class);
            caseViewField = new CaseViewField();
            fieldTypeDefinition = new FieldTypeDefinition();
            fieldTypeDefinition.setId("OrganisationPolicy");
            caseViewField.setFieldTypeDefinition(fieldTypeDefinition);

            caseUpdateViewEvent = CaseUpdateViewEvent.builder()
                .caseFields(new ArrayList<>(Arrays.asList(caseViewField)))
                .eventToken("eventToken")
                .build();

            caseViewEvent = new CaseViewEvent();
            caseViewEvent.setEventId("NOC");
            caseViewResource = new CaseViewResource();
            caseViewResource.setCaseViewEvents(new CaseViewEvent[]{caseViewEvent});

            given(repository.findCaseByCaseId(CASE_ID))
                .willReturn(caseViewResource);
            given(repository.getStartEventTrigger(anyString(), anyString()))
                .willReturn(caseUpdateViewEvent);
            given(jacksonUtils.convertValue(any(CaseViewField.class), any()))
                .willReturn(node);
        }

        @Test
        @DisplayName("Submit Event for Check NoC Approval")
        void checkNoticeOfChangeApproval() {
            service.checkNoticeOfChangeApproval(CASE_ID);
            verify(repository).submitEventForCaseOnly(eq(CASE_ID), any(CaseDataContent.class));
        }

        @Test
        @DisplayName("must return an error when the CaseUpdateViewEvent token is null")
        void shouldThrowErrorWhenCaseUpdateViewEventTokenIsNull() {
            caseUpdateViewEvent.setEventToken(null);

            assertThatThrownBy(() -> service.checkNoticeOfChangeApproval(CASE_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Event token not present");
        }

        @Test
        @DisplayName("must return an error when the CaseViewEvent list is empty")
        void shouldThrowErrorWhenCaseViewEventListIsEmpty() {
            caseViewResource.setCaseViewEvents(new CaseViewEvent[]{});

            assertThatThrownBy(() -> service.checkNoticeOfChangeApproval(CASE_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(NOC_DECISION_EVENT_UNIDENTIFIABLE);
        }

        @Test
        @DisplayName("must return an error when the CaseViewEvent list length is greater than one")
        void shouldThrowErrorWhenCaseViewEventListLengthGreaterThanOne() {
            caseViewResource.setCaseViewEvents(new CaseViewEvent[]{caseViewEvent, caseViewEvent});

            assertThatThrownBy(() -> service.checkNoticeOfChangeApproval(CASE_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(NOC_DECISION_EVENT_UNIDENTIFIABLE);
        }
    }
}
