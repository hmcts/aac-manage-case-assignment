package uk.gov.hmcts.reform.managecase.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.managecase.TestFixtures.CaseUpdateViewEventFixture;
import uk.gov.hmcts.reform.managecase.api.errorhandling.CaseCouldNotBeFetchedException;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseEventCreationPayload;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleResource;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRoleWithOrganisation;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRolesRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.DataStoreApiClient;
import uk.gov.hmcts.reform.managecase.client.datastore.StartEventResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseUpdateViewEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewActionableEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewJurisdiction;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewType;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseUpdateViewEventFixture.CHANGE_ORGANISATION_REQUEST_FIELD;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseUpdateViewEventFixture.getCaseViewFields;
import static uk.gov.hmcts.reform.managecase.TestFixtures.CaseUpdateViewEventFixture.getWizardPages;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.PENDING;
import static uk.gov.hmcts.reform.managecase.repository.DefaultDataStoreRepository.CHANGE_ORGANISATION_REQUEST_MISSING_CASE_FIELD_ID;
import static uk.gov.hmcts.reform.managecase.repository.DefaultDataStoreRepository.NOC_REQUEST_DESCRIPTION;
import static uk.gov.hmcts.reform.managecase.repository.DefaultDataStoreRepository.NOT_ENOUGH_DATA_TO_SUBMIT_START_EVENT;
import static uk.gov.hmcts.reform.managecase.service.CaseAssignmentService.CASE_COULD_NOT_BE_FETCHED;


@SuppressWarnings({"PMD.ExcessiveImports", "PMD.TooManyMethods"})
class DataStoreRepositoryTest {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String ASSIGNEE_ID = "0a5874a4-3f38-4bbd-ba4c";
    private static final String ROLE = "caseworker-probate";
    private static final String CASE_ID = "12345678";
    private static final String CASE_ID2 = "87654321";
    private static final String ORG_ID = "organisation1";
    private static final String EVENT_ID = "NoCRequest";

    private static final String EXPECTED_NOC_REQUEST_DATA =
        "{\"ChangeOrganisationRequest\":"
            + "{"
            +   "\"DummyDataKey\":\"Dummy Data Value\""
            + "}"
            + "}";
    private static final String EVENT_TOKEN = "eventToken";

    public static final String USER_TOKEN = "Bearer user Token";

    private static final String JURISDICTION = "Jurisdiction";

    @Mock
    private DataStoreApiClient dataStoreApi;

    @Mock
    private JacksonUtils jacksonUtils;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private DefaultDataStoreRepository repository;

    @BeforeEach
    void setUp() {
        initMocks(this);
        given(securityUtils.getCaaSystemUserToken()).willReturn(USER_TOKEN);
    }

    @Test
    @DisplayName("find case by id using external facing API")
    void shouldFindCaseByIdUsingExternalApi() {
        // ARRANGE
        CaseDetails caseDetails = CaseDetails.builder()
            .caseTypeId(CASE_TYPE_ID)
            .id(CASE_ID)
            .build();
        given(dataStoreApi.getCaseDetailsByCaseIdViaExternalApi(CASE_ID)).willReturn(caseDetails);

        // ACT
        CaseDetails result = repository.findCaseByCaseIdExternalApi(CASE_ID);

        // ASSERT
        assertThat(result).isEqualTo(caseDetails);
        verify(dataStoreApi).getCaseDetailsByCaseIdViaExternalApi(eq(CASE_ID));
    }

    @Test
    @DisplayName("find case by id using external facing API return no cases")
    void shouldReturnNoCaseForFindCaseByIdUsingExternalApi() {
        // ARRANGE
        given(dataStoreApi.getCaseDetailsByCaseIdViaExternalApi(CASE_ID)).willReturn(null);

        // ACT
        CaseDetails result = repository.findCaseByCaseIdExternalApi(CASE_ID);

        // ASSERT
        assertThat(result).isNull();
        verify(dataStoreApi).getCaseDetailsByCaseIdViaExternalApi(eq(CASE_ID));
    }

    @Test
    @DisplayName("find case by id using external facing API throws CaseCouldNotBeFetchedException")
    void shouldThrowCaseCouldNotBeFetchedExceptionForFindCaseByIdUsingExternalApi() {
        // ARRANGE
        given(dataStoreApi.getCaseDetailsByCaseIdViaExternalApi(CASE_ID))
            .willThrow(new FeignException.NotFound("404",
                                                   Request.create(Request.HttpMethod.GET, "someUrl", Map.of(),
                                                                  null, Charset.defaultCharset(),
                                                                  null
                                                   ), null
            ));

        // ACT & ASSERT
        assertThatThrownBy(() -> repository.findCaseByCaseIdExternalApi(CASE_ID))
            .isInstanceOf(CaseCouldNotBeFetchedException.class)
            .hasMessageContaining(CASE_COULD_NOT_BE_FETCHED);
    }

    @Test
    @DisplayName("Assign case access")
    void shouldAssignCase() {
        // ARRANGE
        doNothing().when(dataStoreApi).assignCase(any(CaseUserRolesRequest.class));

        // ACT
        repository.assignCase(List.of(ROLE), CASE_ID, ASSIGNEE_ID, ORG_ID);

        // ASSERT
        ArgumentCaptor<CaseUserRolesRequest> captor = ArgumentCaptor.forClass(CaseUserRolesRequest.class);
        verify(dataStoreApi).assignCase(captor.capture());
        List<CaseUserRoleWithOrganisation> caseUserRoles = captor.getValue().getCaseUsers();

        assertThat(caseUserRoles.size()).isEqualTo(1);
        CaseUserRoleWithOrganisation caseUserRole = caseUserRoles.get(0);

        assertThat(caseUserRole.getCaseId()).isEqualTo(CASE_ID);
        assertThat(caseUserRole.getCaseRole()).isEqualTo(ROLE);
        assertThat(caseUserRole.getUserId()).isEqualTo(ASSIGNEE_ID);
        assertThat(caseUserRole.getOrganisationId()).isEqualTo(ORG_ID);
    }

    @Test
    @DisplayName("Get case assignments")
    void shouldGetCaseAssignments() {
        // ARRANGE
        List<String> caseIds = List.of(CASE_ID);
        List<String> userIds = List.of(ASSIGNEE_ID);

        CaseUserRole inputRole = CaseUserRole.builder()
                .caseId(CASE_ID)
                .userId(ASSIGNEE_ID)
                .caseRole(ROLE)
                .build();

        given(dataStoreApi.getCaseAssignments(caseIds, userIds))
                .willReturn(new CaseUserRoleResource(List.of(inputRole)));

        // ACT
        List<CaseUserRole> caseUserRoles = repository.getCaseAssignments(caseIds, userIds);

        // ASSERT
        assertThat(caseUserRoles.size()).isEqualTo(1);
        CaseUserRole caseUserRole = caseUserRoles.get(0);

        assertThat(caseUserRole.getCaseId()).isEqualTo(CASE_ID);
        assertThat(caseUserRole.getCaseRole()).isEqualTo(ROLE);
        assertThat(caseUserRole.getUserId()).isEqualTo(ASSIGNEE_ID);
    }

    @Test
    @DisplayName("Remove case user roles")
    void shouldRemoveCaseUserRoles() {
        // ARRANGE
        doNothing().when(dataStoreApi).removeCaseUserRoles(any(CaseUserRolesRequest.class));

        List<CaseUserRole> caseUserRoles = List.of(
            new CaseUserRole(CASE_ID, ASSIGNEE_ID, ROLE),
            new CaseUserRole(CASE_ID2, ASSIGNEE_ID, ROLE)
        );

        // ACT
        repository.removeCaseUserRoles(caseUserRoles, ORG_ID);

        // ASSERT
        ArgumentCaptor<CaseUserRolesRequest> captor = ArgumentCaptor.forClass(CaseUserRolesRequest.class);
        verify(dataStoreApi).removeCaseUserRoles(captor.capture());
        List<CaseUserRoleWithOrganisation> caseUserRolesWithOrganisation = captor.getValue().getCaseUsers();

        assertThat(caseUserRolesWithOrganisation)
            .hasSameSizeAs(caseUserRoles)
            .containsAll(List.of(
                new CaseUserRoleWithOrganisation(CASE_ID, ASSIGNEE_ID, ROLE, ORG_ID),
                new CaseUserRoleWithOrganisation(CASE_ID2, ASSIGNEE_ID, ROLE, ORG_ID)
            ));
    }

    @Test
    @DisplayName("Find case by case and caseId")
    void shouldFindCaseByCaseId() {
        // ARRANGE

        CaseViewActionableEvent caseViewActionableEvent = new CaseViewActionableEvent();
        CaseViewResource caseViewResource = new CaseViewResource();
        caseViewResource.setCaseViewActionableEvents(new CaseViewActionableEvent[] {caseViewActionableEvent});
        CaseViewType caseViewType = new CaseViewType();
        caseViewType.setName(CASE_TYPE_ID);
        caseViewType.setId(CASE_TYPE_ID);
        CaseViewJurisdiction caseViewJurisdiction = new CaseViewJurisdiction();
        caseViewJurisdiction.setId(JURISDICTION);
        caseViewType.setJurisdiction(caseViewJurisdiction);
        caseViewResource.setCaseType(caseViewType);
        given(dataStoreApi.getCaseDetailsByCaseId(anyString(), anyString())).willReturn(caseViewResource);

        // ACT
        CaseViewResource result = repository.findCaseByCaseId(CASE_ID);

        // ASSERT
        assertThat(result).isEqualTo(caseViewResource);
    }

    @Test
    @DisplayName("getStartEventTrigger returns successfully a CaseUpdateViewEvent")
    void shouldReturnCaseUpdateViewEventWhenStartEventTriggerSucceeds() {
        CaseUpdateViewEvent caseUpdateViewEvent = CaseUpdateViewEvent.builder().build();

        given(dataStoreApi.getStartEventTrigger(USER_TOKEN, CASE_ID, EVENT_ID))
            .willReturn(caseUpdateViewEvent);

        CaseUpdateViewEvent returnedCaseUpdateViewEvent
            = repository.getStartEventTrigger(CASE_ID, EVENT_ID);

        assertThat(returnedCaseUpdateViewEvent).isEqualTo(caseUpdateViewEvent);
    }

    @Test
    @DisplayName("getStartEventTrigger returns null")
    void shouldReturnNullCaseResourceOnStartEventTrigger() {
        given(dataStoreApi.getStartEventTrigger(USER_TOKEN, CASE_ID, EVENT_ID))
            .willReturn(null);

        CaseUpdateViewEvent returnedCaseUpdateViewEvent
            = repository.getStartEventTrigger(CASE_ID, EVENT_ID);

        assertThat(returnedCaseUpdateViewEvent).isNull();
    }

    @Test
    @DisplayName("submitEventForCaseOnly returns successfully a CaseDetails")
    void shouldReturnCaseDetailsWhenEventSubmissionSucceeds() {
        CaseEventCreationPayload caseEventCreationPayload = CaseEventCreationPayload.builder().build();
        CaseDetails caseDetails = CaseDetails.builder().build();

        given(dataStoreApi.submitEventForCase(eq(USER_TOKEN), eq(CASE_ID), any(CaseEventCreationPayload.class)))
            .willReturn(caseDetails);

        CaseDetails returnedCaseDetails
            = repository.submitEventForCase(CASE_ID, caseEventCreationPayload);

        assertThat(returnedCaseDetails).isEqualTo(caseDetails);
    }

    @Test
    @DisplayName("submitEventForCaseOnly returns null")
    void shouldReturnNullCaseResourceOnEventSubmission() {
        CaseEventCreationPayload caseEventCreationPayload = CaseEventCreationPayload.builder().build();

        given(dataStoreApi.submitEventForCase(eq(USER_TOKEN), eq(CASE_ID), any(CaseEventCreationPayload.class)))
            .willReturn(null);

        CaseDetails caseDetails
            = repository.submitEventForCase(CASE_ID, caseEventCreationPayload);

        assertThat(caseDetails).isNull();
    }

    @Test
    @DisplayName("Call ccd-datastore where submitting an event for a case fails")
    void shouldReturnNullCaseDetailsWhenSubmittingEventFails() {
        // ARRANGE
        ChangeOrganisationRequest changeOrganisationRequest = ChangeOrganisationRequest.builder().build();

        given(dataStoreApi.getStartEventTrigger(USER_TOKEN, CASE_ID, EVENT_ID))
            .willReturn(null);

        // ACT
        CaseDetails caseDetails
            = repository.submitNoticeOfChangeRequestEvent(CASE_ID, EVENT_ID, changeOrganisationRequest);

        // ASSERT
        verify(dataStoreApi).getStartEventTrigger(USER_TOKEN, CASE_ID, EVENT_ID);
        verify(dataStoreApi, never()).submitEventForCase(any(), any(), any());
        assertThat(caseDetails).isNull();
    }

    @Test
    @DisplayName("Call ccd-datastore to submit an event for a case")
    void shouldReturnCaseDetailsWhenSubmittingEventSucceeds() throws JsonProcessingException {
        // ARRANGE
        CaseUpdateViewEvent caseUpdateViewEvent = CaseUpdateViewEvent.builder()
            .wizardPages(CaseUpdateViewEventFixture.getWizardPages())
            .eventToken(EVENT_TOKEN)
            .caseFields(getCaseViewFields())
            .build();

        given(dataStoreApi.getStartEventTrigger(USER_TOKEN, CASE_ID, EVENT_ID)).willReturn(caseUpdateViewEvent);

        ObjectMapper mapper = new ObjectMapper();

        Map<String, JsonNode> data = new HashMap<>();
        data.put(CHANGE_ORGANISATION_REQUEST_FIELD, mapper.readTree(EXPECTED_NOC_REQUEST_DATA));

        CaseDetails caseDetails = CaseDetails.builder().data(data).build();

        StartEventResource startEventResource = StartEventResource.builder()
            .caseDetails(caseDetails)
            .build();

        given(dataStoreApi.getExternalStartEventTrigger(USER_TOKEN, CASE_ID, EVENT_ID)).willReturn(startEventResource);

        given(dataStoreApi.submitEventForCase(any(String.class), any(String.class),
            any(CaseEventCreationPayload.class))).willReturn(CaseDetails.builder().build());


        given(jacksonUtils.convertValue(any(), any())).willReturn(mapper.readTree(EXPECTED_NOC_REQUEST_DATA));
        doNothing().when(jacksonUtils).merge(any(), eq(data));

        ChangeOrganisationRequest changeOrganisationRequest = ChangeOrganisationRequest.builder()
            .organisationToAdd(Organisation.builder().organisationID("orgNameToAdd").build())
            .organisationToRemove(Organisation.builder().organisationID("orgNameToRemove").build())
            .requestTimestamp(LocalDateTime.now())
            .build();

        // ACT
        repository.submitNoticeOfChangeRequestEvent(CASE_ID, EVENT_ID, changeOrganisationRequest);

        // ASSERT
        verify(dataStoreApi).getStartEventTrigger(USER_TOKEN, CASE_ID, EVENT_ID);
        ArgumentCaptor<CaseEventCreationPayload> captor = ArgumentCaptor.forClass(CaseEventCreationPayload.class);
        verify(dataStoreApi).submitEventForCase(any(String.class), any(String.class), captor.capture());

        CaseEventCreationPayload caseEventCreationPayloadCaptorValue = captor.getValue();
        assertThat(caseEventCreationPayloadCaptorValue.getToken()).isEqualTo(EVENT_TOKEN);

        assertThat(caseEventCreationPayloadCaptorValue.getEvent().getDescription()).isEqualTo(NOC_REQUEST_DESCRIPTION);
        assertThat(caseEventCreationPayloadCaptorValue.getEvent().getEventId()).isEqualTo(EVENT_ID);

        assertThat(caseEventCreationPayloadCaptorValue.getData().get(CHANGE_ORGANISATION_REQUEST_FIELD).toString())
            .isEqualTo(EXPECTED_NOC_REQUEST_DATA);
    }

    @Test
    @DisplayName("handle missing event token when calling submit event for case")
    void shouldThrowExceptionWhenSubmitEventForCaseCalledWithoutEventToken() {

        // ARRANGE
        CaseUpdateViewEvent caseUpdateViewEvent = CaseUpdateViewEvent.builder()
            .caseFields(getCaseViewFields())
            .wizardPages(getWizardPages("testCVaseField"))
            .build();

        given(dataStoreApi.getStartEventTrigger(USER_TOKEN, CASE_ID, EVENT_ID)).willReturn(caseUpdateViewEvent);

        // ACT & ASSERT
        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () ->
            repository.submitNoticeOfChangeRequestEvent(CASE_ID, EVENT_ID, ChangeOrganisationRequest.builder().build())
        );

        assertThat(illegalStateException.getMessage())
            .isEqualTo(NOT_ENOUGH_DATA_TO_SUBMIT_START_EVENT);

        verify(dataStoreApi, never()).submitEventForCase(any(), any(), any());
    }

    @Test
    @DisplayName("handle missing Change Organisation Request case view field when calling submit event for case")
    void shouldThrowExceptionWhenSubmitEventForCaseCalledWithoutCaseViewField() {

        // ARRANGE
        given(dataStoreApi.getStartEventTrigger(USER_TOKEN, CASE_ID, EVENT_ID))
            .willReturn(CaseUpdateViewEvent.builder()
                            .eventToken("eventToken")
                            .build());

        // ACT & ASSERT
        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () ->
            repository.submitNoticeOfChangeRequestEvent(CASE_ID, EVENT_ID, ChangeOrganisationRequest.builder().build())
        );

        assertThat(illegalStateException.getMessage()).isEqualTo(CHANGE_ORGANISATION_REQUEST_MISSING_CASE_FIELD_ID);

        // ASSERT
        verify(dataStoreApi).getStartEventTrigger(USER_TOKEN, CASE_ID, EVENT_ID);
        verify(dataStoreApi, never()).submitEventForCase(any(), any(), any());
    }

    @Test
    @DisplayName("handle missing default approval status in case data when calling submit event for case")
    void shouldSetDefaultValueForCORApprovalSubmitEventForCaseCalled() throws JsonProcessingException {

        // ARRANGE
        CaseUpdateViewEvent caseUpdateViewEvent = CaseUpdateViewEvent.builder()
            .wizardPages(CaseUpdateViewEventFixture.getWizardPages())
            .eventToken(EVENT_TOKEN)
            .caseFields(getCaseViewFields())
            .build();

        given(dataStoreApi.getStartEventTrigger(USER_TOKEN, CASE_ID, EVENT_ID)).willReturn(caseUpdateViewEvent);

        StartEventResource startEventResource = StartEventResource.builder()
            .caseDetails(CaseDetails.builder().data(new HashMap<>()).build())
            .build();
        given(dataStoreApi.getExternalStartEventTrigger(USER_TOKEN, CASE_ID, EVENT_ID)).willReturn(startEventResource);

        given(dataStoreApi.submitEventForCase(any(String.class),
                                              any(String.class),
                                              any(CaseEventCreationPayload.class)))
            .willReturn(CaseDetails.builder().build());

        ArgumentCaptor<ChangeOrganisationRequest> corCaptor = ArgumentCaptor.forClass(ChangeOrganisationRequest.class);
        given(jacksonUtils.convertValue(corCaptor.capture(), any())).willReturn(null);

        // ACT
        repository.submitNoticeOfChangeRequestEvent(CASE_ID, EVENT_ID, ChangeOrganisationRequest.builder().build());

        // ASSERT
        assertThat(PENDING.getValue()).isEqualTo(corCaptor.getValue().getApprovalStatus());
    }
}
