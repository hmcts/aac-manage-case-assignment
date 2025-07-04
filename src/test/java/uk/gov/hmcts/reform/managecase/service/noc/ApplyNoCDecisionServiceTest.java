package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.validation.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.managecase.api.payload.ApplyNoCDecisionRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.prd.ContactInformation;
import uk.gov.hmcts.reform.managecase.client.prd.FindOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.AddressUK;
import uk.gov.hmcts.reform.managecase.domain.PreviousOrganisation;
import uk.gov.hmcts.reform.managecase.domain.PreviousOrganisationCollectionItem;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequest;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.service.NotifyService;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.COR_MISSING;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_REQUEST_NOT_CONSIDERED;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_DATA_PROVIDED;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.UNKNOWN_NOC_APPROVAL_STATUS;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.APPROVAL_STATUS;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.CASE_ROLE_ID;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION_TO_REMOVE;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORG_POLICY_CASE_ASSIGNED_ROLE;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.PENDING;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.REJECTED;

@SuppressWarnings({"PMD.ExcessiveImports", "PMD.TooManyMethods", "PMD.DataflowAnomalyAnalysis",
    "PMD.JUnitAssertionsShouldIncludeMessage"})
class ApplyNoCDecisionServiceTest {

    private static final String CASE_ID = "123";
    private static final String ORG_1_ID = "Org1Id";
    private static final String ORG_2_ID = "Org2Id";
    private static final String ORG_3_ID = "Org3Id";
    private static final String ORG_1_NAME = "Org1Name";
    private static final String ORG_2_NAME = "Org2Name";
    private static final String ORG_3_NAME = "Org3Name";
    private static final String ORG_POLICY_1_REF = "DefendantPolicy";
    private static final String ORG_POLICY_2_REF = "ClaimantPolicy";
    private static final String ORG_POLICY_1_ROLE = "[Defendant]";
    private static final String ORG_POLICY_2_ROLE = "[Claimant]";
    private static final String CHANGE_ORG_REQUEST_FIELD = "ChangeOrganisationRequestField";
    private static final String ORG_POLICY_1_FIELD = "OrganisationPolicyField1";
    private static final String ORG_POLICY_2_FIELD = "OrganisationPolicyField2";
    private static final String USER_ID_1 = "UserId1";
    private static final String USER_ID_2 = "UserId2";
    private static final String USER_ID_3 = "UserId3";
    private static final String PREVIOUS_ORGANISATIONS = "PreviousOrganisations";
    private final ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private ApplyNoCDecisionService applyNoCDecisionService;
    @Mock
    private PrdRepository prdRepository;
    @Mock
    private DataStoreRepository dataStoreRepository;
    @Mock
    private NotifyService notifyService;
    @Captor
    private ArgumentCaptor<List<CaseUserRole>> caseUserRolesCaptor;
    @Captor
    private ArgumentCaptor<List<EmailNotificationRequest>> emailRequestsCaptor;
    @Captor
    private ArgumentCaptor<List<String>> caseRolesCaptor;

    @BeforeEach
    void setUp() {
        mapper.registerModule(new JavaTimeModule());
        MockitoAnnotations.openMocks(this);
        applyNoCDecisionService = new ApplyNoCDecisionService(prdRepository, dataStoreRepository,
            notifyService, new JacksonUtils(mapper), mapper);
        when(prdRepository.findOrganisationAddress(ArgumentMatchers.any()))
            .thenReturn(new FindOrganisationResponse(emptyList(), ORG_2_ID, ORG_2_NAME));
    }

    @Test
    void shouldUpdateCaseDataWhenRemoveDecisionIsApplied() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createData())
            .id(CASE_ID)
            .build();

        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null)).thenReturn(emptyList());
        when(prdRepository.findUsersByOrganisation(ORG_2_ID))
            .thenReturn(new FindUsersByOrganisationResponse(emptyList(), ORG_2_ID));

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        Map<String, JsonNode> result = applyNoCDecisionService.applyNoCDecision(request);

        assertAll(
            () -> assertThat(result.get(CHANGE_ORG_REQUEST_FIELD).toString(), is(emptyChangeOrgRequestField())),
            () -> assertThat(result.get(ORG_POLICY_1_FIELD).toString(),
                is(orgPolicyAsString(ORG_1_ID, ORG_1_NAME,
                    ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null))),
            () -> assertThat(result.get(ORG_POLICY_2_FIELD).toString(),
                is(orgPolicyAsString(null, null,
                    ORG_POLICY_2_REF, ORG_POLICY_2_ROLE, null)))
        );
    }

    @Test
    void shouldRemoveExistingOrgUsersWithAccessWhenRemoveDecisionIsApplied() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createData())
            .id(CASE_ID)
            .build();

        List<CaseUserRole> existingCaseAssignments = List.of(
            new CaseUserRole(CASE_ID, USER_ID_1, ORG_POLICY_2_ROLE),
            new CaseUserRole(CASE_ID, USER_ID_2, ORG_POLICY_2_ROLE),
            new CaseUserRole(CASE_ID, USER_ID_3, ORG_POLICY_2_ROLE)
        );
        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null))
            .thenReturn(existingCaseAssignments);

        FindUsersByOrganisationResponse usersByOrganisation = new FindUsersByOrganisationResponse(List.of(
            prdUser(1), prdUser(3), prdUser(4)), ORG_2_ID);
        when(prdRepository.findUsersByOrganisation(ORG_2_ID)).thenReturn(usersByOrganisation);

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        applyNoCDecisionService.applyNoCDecision(request);

        assertAll(
            () -> verify(dataStoreRepository).removeCaseUserRoles(caseUserRolesCaptor.capture(),
                Mockito.eq(ORG_2_ID)),
            () -> assertThat(caseUserRolesCaptor.getValue().size(), is(2)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(0).getCaseId(), is(CASE_ID)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(0).getUserId(), is(USER_ID_1)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(0).getCaseRole(), is(ORG_POLICY_2_ROLE)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(1).getCaseId(), is(CASE_ID)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(1).getUserId(), is(USER_ID_3)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(1).getCaseRole(), is(ORG_POLICY_2_ROLE)),
            () -> verify(notifyService).sendEmail(emailRequestsCaptor.capture()),
            () -> assertThat(emailRequestsCaptor.getValue().size(), is(2)),
            () -> assertThat(emailRequestsCaptor.getValue().get(0).getCaseId(), is(CASE_ID)),
            () -> assertThat(emailRequestsCaptor.getValue().get(0).getEmailAddress(), is("User1Email")),
            () -> assertThat(emailRequestsCaptor.getValue().get(1).getCaseId(), is(CASE_ID)),
            () -> assertThat(emailRequestsCaptor.getValue().get(1).getEmailAddress(), is("User3Email")),
            () -> verify(dataStoreRepository, never())
                .assignCase(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())
        );
    }

    @Test
    void shouldRemoveOneExistingOrgUsersWithAccessWhenRemoveDecisionIsApplied() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createSingleOrgData())
            .id(CASE_ID)
            .build();

        List<CaseUserRole> existingCaseAssignments = List.of(
            new CaseUserRole(CASE_ID, USER_ID_1, ORG_POLICY_1_ROLE),
            new CaseUserRole(CASE_ID, USER_ID_1, ORG_POLICY_2_ROLE)
        );
        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null))
            .thenReturn(existingCaseAssignments);

        FindUsersByOrganisationResponse usersByOrganisation = new FindUsersByOrganisationResponse(List.of(
            prdUser(1)), ORG_1_ID);
        when(prdRepository.findUsersByOrganisation(ORG_1_ID)).thenReturn(usersByOrganisation);

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        applyNoCDecisionService.applyNoCDecision(request);

        assertAll(
            () -> verify(dataStoreRepository).removeCaseUserRoles(caseUserRolesCaptor.capture(),
                Mockito.eq(ORG_1_ID)),
            () -> assertThat(caseUserRolesCaptor.getValue().size(), is(1)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(0).getCaseId(), is(CASE_ID)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(0).getUserId(), is(USER_ID_1)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(0).getCaseRole(), is(ORG_POLICY_2_ROLE)),
            () -> verify(notifyService).sendEmail(emailRequestsCaptor.capture()),
            () -> assertThat(emailRequestsCaptor.getValue().size(), is(1)),
            () -> assertThat(emailRequestsCaptor.getValue().get(0).getCaseId(), is(CASE_ID)),
            () -> assertThat(emailRequestsCaptor.getValue().get(0).getEmailAddress(), is("User1Email")),
            () -> verify(dataStoreRepository, never())
                .assignCase(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())
        );
    }

    @Test
    void shouldUpdateCaseDataWhenAddDecisionIsApplied() throws JsonProcessingException {
        LocalDateTime dateTime = LocalDateTime.now();
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createData(
                orgPolicyAsString(null, null, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null),
                orgPolicyAsString(null, null, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE, null),
                organisationAsString(ORG_3_ID, ORG_3_NAME),
                organisationAsString(null, null)
            ))
            .id(CASE_ID)
            .createdDate(dateTime)
            .build();

        when(prdRepository.findUsersByOrganisation(ORG_3_ID))
            .thenReturn(new FindUsersByOrganisationResponse(emptyList(), ORG_3_ID));

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        Map<String, JsonNode> result = applyNoCDecisionService.applyNoCDecision(request);
        ArrayNode arrayNode = (ArrayNode) result.get(ORG_POLICY_2_FIELD).findValue(PREVIOUS_ORGANISATIONS);

        assertAll(
            () -> assertThat(result.get(CHANGE_ORG_REQUEST_FIELD).toString(), is(emptyChangeOrgRequestField())),
            () -> assertThat(
                result.get(ORG_POLICY_1_FIELD).toString(),
                is(orgPolicyAsString(null, null,
                    ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null))),
            () -> assertNotNull(arrayNode),
            () -> assertThat(arrayNode.size(), is(1)),
            () -> assertFalse(arrayNode.get(0).findValue("ToTimestamp").isNull()),
            () -> assertFalse(arrayNode.get(0).findValue("FromTimestamp").isNull()),
            () -> assertTrue(arrayNode.get(0).findValue("OrganisationAddress").isNull()),
            () -> assertTrue(arrayNode.get(0).findValue("OrganisationName").isNull())
        );
    }

    @Test
    void shouldUpdateOrgUsersAccessWhenAddDecisionIsApplied() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createData(
                orgPolicyAsString(null, null, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null),
                orgPolicyAsString(null, null, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE, null),
                organisationAsString(ORG_3_ID, ORG_3_NAME),
                organisationAsString(null, null)
            ))
            .id(CASE_ID)
            .build();

        String otherRole = "OtherRole";
        List<CaseUserRole> existingCaseAssignments = List.of(
            new CaseUserRole(CASE_ID, USER_ID_1, otherRole),
            new CaseUserRole(CASE_ID, USER_ID_2, otherRole),
            new CaseUserRole(CASE_ID, USER_ID_3, otherRole)
        );
        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null))
            .thenReturn(existingCaseAssignments);

        FindUsersByOrganisationResponse usersByOrganisation = new FindUsersByOrganisationResponse(List.of(
            prdUser(1), prdUser(3)), ORG_3_ID);
        when(prdRepository.findUsersByOrganisation(ORG_3_ID)).thenReturn(usersByOrganisation);

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        applyNoCDecisionService.applyNoCDecision(request);

        assertAll(
            () -> verify(dataStoreRepository, times(2))
                .assignCase(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()),
            () -> verify(dataStoreRepository).assignCase(Mockito.any(), Mockito.eq(CASE_ID),
                Mockito.eq(USER_ID_1), Mockito.eq(ORG_3_ID)),
            () -> verify(dataStoreRepository).assignCase(caseRolesCaptor.capture(), Mockito.eq(CASE_ID),
                Mockito.eq(USER_ID_3), Mockito.eq(ORG_3_ID)),
            () -> assertThat(caseRolesCaptor.getValue().size(), is(1)),
            () -> assertThat(caseRolesCaptor.getValue().get(0), is(ORG_POLICY_2_ROLE)),
            () -> verify(dataStoreRepository, never()).removeCaseUserRoles(Mockito.any(), Mockito.any()),
            () -> verify(notifyService, never()).sendEmail(Mockito.any())
        );
    }

    @Test
    void shouldUpdateCaseDataWhenReplaceDecisionIsApplied() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createData(
                orgPolicyAsString(ORG_1_ID, ORG_1_NAME, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null),
                orgPolicyAsString(ORG_2_ID, ORG_2_NAME, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE, null),
                organisationAsString(ORG_3_ID, ORG_3_NAME),
                organisationAsString(ORG_2_ID, ORG_2_NAME)
            ))
            .id(CASE_ID)
            .build();

        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null)).thenReturn(emptyList());
        when(prdRepository.findUsersByOrganisation(ORG_2_ID))
            .thenReturn(new FindUsersByOrganisationResponse(emptyList(), ORG_2_ID));
        when(prdRepository.findUsersByOrganisation(ORG_3_ID))
            .thenReturn(new FindUsersByOrganisationResponse(emptyList(), ORG_3_ID));

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        Map<String, JsonNode> result = applyNoCDecisionService.applyNoCDecision(request);

        assertAll(
            () -> assertThat(result.get(CHANGE_ORG_REQUEST_FIELD).toString(), is(emptyChangeOrgRequestField())),
            () -> assertThat(result.get(ORG_POLICY_1_FIELD).toString(),
                is(orgPolicyAsString(ORG_1_ID, ORG_1_NAME,
                    ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null))),
            () -> assertThat(result.get(ORG_POLICY_2_FIELD).toString(),
                is(orgPolicyAsString(ORG_3_ID, ORG_3_NAME,
                    ORG_POLICY_2_REF, ORG_POLICY_2_ROLE, "userEmail")))
        );
    }

    @Test
    void shouldUpdateOrgUsersAccessWhenReplaceDecisionIsApplied1() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createData(
                orgPolicyAsString(null, null, null, null, "userOne"),
                orgPolicyAsString(ORG_2_ID, ORG_2_NAME, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE, "userOne"),
                organisationAsString(ORG_3_ID, ORG_3_NAME),
                organisationAsString(ORG_2_ID, ORG_2_NAME)
            ))
            .id(CASE_ID)
            .build();

        List<CaseUserRole> existingCaseAssignments = List.of(
            new CaseUserRole(CASE_ID, USER_ID_1, ORG_POLICY_2_ROLE),
            new CaseUserRole(CASE_ID, USER_ID_2, ORG_POLICY_2_ROLE),
            new CaseUserRole(CASE_ID, USER_ID_3, ORG_POLICY_2_ROLE)
        );
        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null))
            .thenReturn(existingCaseAssignments);

        FindUsersByOrganisationResponse usersByOrganisation1 = new FindUsersByOrganisationResponse(List.of(
            prdUser(2), prdUser(3), prdUser(4)), ORG_2_ID);
        when(prdRepository.findUsersByOrganisation(ORG_2_ID)).thenReturn(usersByOrganisation1);

        FindUsersByOrganisationResponse usersByOrganisation2 = new FindUsersByOrganisationResponse(List.of(
            prdUser(3), prdUser(5)), ORG_3_ID);
        when(prdRepository.findUsersByOrganisation(ORG_3_ID)).thenReturn(usersByOrganisation2);

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        applyNoCDecisionService.applyNoCDecision(request);

        assertAll(
            () -> verify(dataStoreRepository).removeCaseUserRoles(caseUserRolesCaptor.capture(), Mockito.eq(ORG_2_ID)),
            () -> assertThat(caseUserRolesCaptor.getValue().size(), is(1)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(0).getCaseId(), is(CASE_ID)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(0).getUserId(), is(USER_ID_2)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(0).getCaseRole(), is(ORG_POLICY_2_ROLE)),
            () -> verify(dataStoreRepository, never()).removeCaseUserRoles(Mockito.any(), Mockito.eq(ORG_1_ID)),
            () -> verify(dataStoreRepository, never()).removeCaseUserRoles(Mockito.any(), Mockito.eq(ORG_3_ID)),
            () -> verify(notifyService).sendEmail(emailRequestsCaptor.capture()),
            () -> assertThat(emailRequestsCaptor.getValue().size(), is(1)),
            () -> assertThat(emailRequestsCaptor.getValue().get(0).getCaseId(), is(CASE_ID)),
            () -> assertThat(emailRequestsCaptor.getValue().get(0).getEmailAddress(), is("User2Email")),
            () -> verify(dataStoreRepository).assignCase(caseRolesCaptor.capture(), Mockito.eq(CASE_ID),
                Mockito.eq(USER_ID_3), Mockito.eq(ORG_3_ID)),
            () -> assertThat(caseRolesCaptor.getValue().size(), is(1)),
            () -> assertThat(caseRolesCaptor.getValue().get(0), is(ORG_POLICY_2_ROLE))
        );
    }

    @Test
    void shouldUpdateOrgUsersAccessWhenReplaceDecisionIsApplied2() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createData(
                orgPolicyAsString(null, null, null, null, null),
                orgPolicyAsString(ORG_2_ID, ORG_2_NAME, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE, null),
                organisationAsString(ORG_1_ID, ORG_1_NAME),
                organisationAsString(ORG_2_ID, ORG_2_NAME)
            ))
            .id(CASE_ID)
            .build();

        List<CaseUserRole> existingCaseAssignments = List.of(
            new CaseUserRole(CASE_ID, USER_ID_1, ORG_POLICY_2_ROLE),
            new CaseUserRole(CASE_ID, USER_ID_2, ORG_POLICY_2_ROLE)
        );
        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null))
            .thenReturn(existingCaseAssignments);

        FindUsersByOrganisationResponse usersByOrganisation1 = new FindUsersByOrganisationResponse(List.of(
            prdUser(1)), ORG_1_ID);
        when(prdRepository.findUsersByOrganisation(ORG_1_ID)).thenReturn(usersByOrganisation1);

        FindUsersByOrganisationResponse usersByOrganisation2 = new FindUsersByOrganisationResponse(List.of(
            prdUser(1), prdUser(2)), ORG_2_ID);
        when(prdRepository.findUsersByOrganisation(ORG_2_ID)).thenReturn(usersByOrganisation2);

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        applyNoCDecisionService.applyNoCDecision(request);
        assertAll(
            () -> verify(dataStoreRepository).removeCaseUserRoles(caseUserRolesCaptor.capture(), Mockito.eq(ORG_2_ID)),
            () -> assertThat(caseUserRolesCaptor.getValue().size(), is(1)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(0).getCaseId(), is(CASE_ID)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(0).getUserId(), is(USER_ID_2)),
            () -> assertThat(caseUserRolesCaptor.getValue().get(0).getCaseRole(), is(ORG_POLICY_2_ROLE)),
            () -> verify(dataStoreRepository, never()).removeCaseUserRoles(Mockito.any(), Mockito.eq(ORG_1_ID)),
            () -> verify(notifyService).sendEmail(emailRequestsCaptor.capture()),
            () -> assertThat(emailRequestsCaptor.getValue().size(), is(1)),
            () -> assertThat(emailRequestsCaptor.getValue().get(0).getCaseId(), is(CASE_ID)),
            () -> assertThat(emailRequestsCaptor.getValue().get(0).getEmailAddress(), is("User2Email")),
            () -> verify(dataStoreRepository).assignCase(caseRolesCaptor.capture(), Mockito.eq(CASE_ID),
                Mockito.eq(USER_ID_1), Mockito.eq(ORG_1_ID)),
            () -> assertThat(caseRolesCaptor.getValue().size(), is(1)),
            () -> assertThat(caseRolesCaptor.getValue().get(0), is(ORG_POLICY_2_ROLE))
        );
    }

    @Test
    void shouldUpdateCaseDataWhenRejectedDecisionIsApplied() throws JsonProcessingException {
        Map<String, JsonNode> data = createData();
        ((ObjectNode) data.get(CHANGE_ORG_REQUEST_FIELD)).set(APPROVAL_STATUS, new TextNode(REJECTED.getValue()) {
        });
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .id(CASE_ID)
            .build();

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        Map<String, JsonNode> result = applyNoCDecisionService.applyNoCDecision(request);

        assertAll(
            () -> assertThat(result.get(CHANGE_ORG_REQUEST_FIELD).toString(), is(emptyChangeOrgRequestField())),
            () -> assertThat(result.get(ORG_POLICY_1_FIELD).toString(),
                is(orgPolicyAsString(ORG_1_ID, ORG_1_NAME,
                    ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null))),
            () -> assertThat(result.get(ORG_POLICY_2_FIELD).toString(),
                is(orgPolicyAsString(ORG_2_ID, ORG_2_NAME,
                    ORG_POLICY_2_REF, ORG_POLICY_2_ROLE, null)))
        );
    }

    @Test
    void shouldErrorWhenApprovalStatusIsNotSet() throws JsonProcessingException {
        Map<String, JsonNode> data = createData();
        ((ObjectNode) data.get(CHANGE_ORG_REQUEST_FIELD)).set(APPROVAL_STATUS, mapper.nullNode());
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .id(CASE_ID)
            .build();

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> applyNoCDecisionService.applyNoCDecision(request));

        assertAll(
            () -> assertThat(exception.getMessage(), is("A value is expected for 'ApprovalStatus'"))
        );
    }

    @Test
    void shouldErrorWhenApprovalStatusIsPending() throws JsonProcessingException {
        Map<String, JsonNode> data = createData();
        ((ObjectNode) data.get(CHANGE_ORG_REQUEST_FIELD)).set(APPROVAL_STATUS,
            new TextNode(PENDING.getValue()));
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .id(CASE_ID)
            .build();

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> applyNoCDecisionService.applyNoCDecision(request));

        assertAll(
            () -> assertThat(exception.getMessage(), is(NOC_REQUEST_NOT_CONSIDERED))
        );
    }

    @Test
    void shouldErrorWhenApprovalStatusIsInvalid() throws JsonProcessingException {
        Map<String, JsonNode> data = createData();
        ((ObjectNode) data.get(CHANGE_ORG_REQUEST_FIELD)).set(APPROVAL_STATUS, new TextNode("5"));
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .id(CASE_ID)
            .build();

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> applyNoCDecisionService.applyNoCDecision(request));

        assertAll(
            () -> assertThat(exception.getMessage(), is(UNKNOWN_NOC_APPROVAL_STATUS))
        );
    }

    @Test
    void shouldErrorWhenCaseRoleIdIsNotSet() throws JsonProcessingException {
        Map<String, JsonNode> data = createData();
        ((ObjectNode) data.get(CHANGE_ORG_REQUEST_FIELD)).set(CASE_ROLE_ID, mapper.nullNode());
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .id(CASE_ID)
            .build();

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> applyNoCDecisionService.applyNoCDecision(request));

        assertAll(
            () -> assertThat(exception.getMessage(), is("A value is expected for 'CaseRoleId.value.code'"))
        );
    }

    @Test
    void shouldErrorWhenNoOrgPolicyExistsForCaseRoleId() throws JsonProcessingException {
        Map<String, JsonNode> data = createData();
        ((ObjectNode) data.get(CHANGE_ORG_REQUEST_FIELD)).set(CASE_ROLE_ID,
            mapper.readTree(caseRoleIdField("UnknownCaseRoleId")));
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .id(CASE_ID)
            .build();

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> applyNoCDecisionService.applyNoCDecision(request));

        assertAll(
            () -> assertThat(exception.getMessage(),
                is("No Organisation Policy found with case role ID 'UnknownCaseRoleId'"))
        );
    }

    @Test
    void shouldErrorWhenMultipleOrgPoliciesExistForCaseRoleId() throws JsonProcessingException {
        Map<String, JsonNode> data = createData();
        ((ObjectNode) data.get(ORG_POLICY_1_FIELD)).set(ORG_POLICY_CASE_ASSIGNED_ROLE, new TextNode(ORG_POLICY_2_ROLE));
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .id(CASE_ID)
            .build();

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> applyNoCDecisionService.applyNoCDecision(request));

        assertAll(
            () -> assertThat(exception.getMessage(),
                is("More than one Organisation Policy with case role ID '[Claimant]' exists on case"))
        );
    }

    @Test
    void shouldErrorWhenChangeOrganisationRequestIsNotPresent() throws JsonProcessingException {
        Map<String, JsonNode> data = createData();
        data.remove(CHANGE_ORG_REQUEST_FIELD);
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .id(CASE_ID)
            .build();

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> applyNoCDecisionService.applyNoCDecision(request));

        assertAll(
            () -> assertThat(exception.getMessage(), is(COR_MISSING))
        );
    }

    @Test
    void shouldErrorWhenOrganisationToRemoveIsNotPresent() throws JsonProcessingException {
        Map<String, JsonNode> data = createData();
        ((ObjectNode) data.get(CHANGE_ORG_REQUEST_FIELD)).remove(ORGANISATION_TO_REMOVE);
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .id(CASE_ID)
            .build();

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> applyNoCDecisionService.applyNoCDecision(request));

        assertAll(
            () -> assertThat(exception.getMessage(),
                is("Fields of type ChangeOrganisationRequest must include both "
                   + "an OrganisationToAdd and OrganisationToRemove field."))
        );
    }

    @Test
    void shouldErrorWhenDataIsNotPresent() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
            .id(CASE_ID)
            .build();

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> applyNoCDecisionService.applyNoCDecision(request));

        assertAll(
            () -> assertThat(exception.getMessage(), is(NO_DATA_PROVIDED))
        );
    }

    @Test
    void shouldUpdatePreviousOrganisations() throws JsonProcessingException {
        LocalDateTime dateTime = LocalDateTime.now();
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createData())
            .createdDate(dateTime)
            .id(CASE_ID)
            .build();

        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null)).thenReturn(emptyList());
        when(prdRepository.findUsersByOrganisation(ORG_2_ID))
            .thenReturn(new FindUsersByOrganisationResponse(emptyList(), ORG_2_ID));

        when(prdRepository.findOrganisationAddress(ORG_2_ID))
            .thenReturn(new FindOrganisationResponse(Lists.newArrayList(orgContactInformation()),
                ORG_2_ID, ORG_2_NAME));

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        Map<String, JsonNode> result = applyNoCDecisionService.applyNoCDecision(request);
        ArrayNode arrayNode = (ArrayNode) result.get(ORG_POLICY_2_FIELD).findValue(PREVIOUS_ORGANISATIONS);

        assertAll(
            () -> assertThat(result.get(CHANGE_ORG_REQUEST_FIELD).toString(), is(emptyChangeOrgRequestField())),
            () -> assertThat(result.get(ORG_POLICY_1_FIELD).toString(),
                is(orgPolicyAsString(ORG_1_ID, ORG_1_NAME,
                    ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null))),
            () -> assertNotNull(arrayNode),
            () -> assertNotNull(arrayNode.get(0).findValue("FromTimestamp")),
            () -> assertNotNull(arrayNode.get(0).findValue("ToTimestamp")),
            () -> assertThat(arrayNode.get(0).findValue("OrganisationName").textValue(), is(ORG_2_NAME)),
            () -> assertNotNull(arrayNode.get(0).findValue("OrganisationAddress"))
        );
    }

    @Test
    void shouldUpdatePreviousOrganisationsWhenOnePreviousOrgExists() throws JsonProcessingException {
        LocalDate fromDate = LocalDate.of(2020, Month.DECEMBER, 1);
        LocalDate toDate = LocalDate.of(2020, Month.DECEMBER, 3);
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createPreviousOrgData(Lists.newArrayList(createPreviousOrganisation(fromDate, toDate))))
            .createdDate(LocalDateTime.now())
            .id(CASE_ID)
            .build();

        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null)).thenReturn(emptyList());
        when(prdRepository.findUsersByOrganisation(ORG_2_ID))
            .thenReturn(new FindUsersByOrganisationResponse(emptyList(), ORG_2_ID));

        when(prdRepository.findOrganisationAddress(ORG_2_ID))
            .thenReturn(new FindOrganisationResponse(Lists.newArrayList(orgContactInformation()),
                ORG_2_ID, ORG_2_NAME));

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        Map<String, JsonNode> result = applyNoCDecisionService.applyNoCDecision(request);
        ArrayNode arrayNode = (ArrayNode) result.get(ORG_POLICY_2_FIELD).findValue(PREVIOUS_ORGANISATIONS);

        assertAll(
            () -> assertThat(result.get(CHANGE_ORG_REQUEST_FIELD).toString(), is(emptyChangeOrgRequestField())),
            () -> assertThat(
                result.get(ORG_POLICY_1_FIELD).toString(),
                is(orgPolicyAsString(ORG_1_ID, ORG_1_NAME,
                    ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null
                ))
            ),
            () -> assertNotNull(arrayNode),
            () -> assertThat(arrayNode.size(), is(2)),
            () -> assertNotNull(arrayNode.get(0).findValue("FromTimestamp")),
            () -> assertNotNull(arrayNode.get(0).findValue("ToTimestamp")),
            () -> assertNotNull(arrayNode.get(0).findValue("OrganisationAddress")),
            () -> assertThat(arrayNode.get(0).findValue("OrganisationName").textValue(), is(ORG_2_NAME))
        );
    }

    @Test
    void shouldUpdatePreviousOrganisationWithFronAndToTimeStampsOnlyAfterInitialApplyNoCDecision()
        throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createAddOrgData())
            .createdDate(LocalDateTime.now())
            .id(CASE_ID)
            .build();

        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null)).thenReturn(emptyList());
        when(prdRepository.findUsersByOrganisation(ORG_2_ID))
            .thenReturn(new FindUsersByOrganisationResponse(emptyList(), ORG_2_ID));

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        Map<String, JsonNode> result = applyNoCDecisionService.applyNoCDecision(request);
        ArrayNode arrayNode = (ArrayNode) result.get(ORG_POLICY_2_FIELD).findValue(PREVIOUS_ORGANISATIONS);

        assertAll(
            () -> assertThat(result.get(CHANGE_ORG_REQUEST_FIELD).toString(), is(emptyChangeOrgRequestField())),
            () -> assertThat(
                result.get(ORG_POLICY_1_FIELD).toString(),
                is(orgPolicyAsString(ORG_1_ID, ORG_1_NAME,
                    ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null
                ))
            ),
            () -> assertNotNull(arrayNode),
            () -> assertThat(arrayNode.size(), is(1)),
            () -> assertNotNull(arrayNode.get(0).findValue("FromTimestamp")),
            () -> assertNotNull(arrayNode.get(0).findValue("ToTimestamp")),
            () -> assertTrue(arrayNode.get(0).findValue("OrganisationAddress").isNull()),
            () -> assertTrue(arrayNode.get(0).findValue("OrganisationName").isNull())
        );
    }

    @Test
    void shouldUpdatePreviousOrganisationsWhenMoreThanOnePreviousOrgExists() throws IOException {
        LocalDate fromDate = LocalDate.of(2020, Month.DECEMBER, 1);
        LocalDate toDate = LocalDate.of(2020, Month.DECEMBER, 2);

        LocalDate fromDate2 = LocalDate.of(2020, Month.DECEMBER, 2);
        LocalDate toDate2 = LocalDate.of(2020, Month.DECEMBER, 3);
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createPreviousOrgData(Lists.newArrayList(createPreviousOrganisation(fromDate, toDate),
                createPreviousOrganisation(fromDate2, toDate2))))
            .createdDate(LocalDateTime.now())
            .id(CASE_ID)
            .build();

        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null)).thenReturn(emptyList());
        when(prdRepository.findUsersByOrganisation(ORG_2_ID))
            .thenReturn(new FindUsersByOrganisationResponse(emptyList(), ORG_2_ID));

        when(prdRepository.findOrganisationAddress(ORG_2_ID))
            .thenReturn(new FindOrganisationResponse(Lists.newArrayList(orgContactInformation()),
                ORG_2_ID, ORG_2_NAME));

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        Map<String, JsonNode> result = applyNoCDecisionService.applyNoCDecision(request);

        JsonNode prevOrgsNode = result.get(ORG_POLICY_2_FIELD).findValue(PREVIOUS_ORGANISATIONS);
        List<PreviousOrganisationCollectionItem> previousOrganisations = mapper
            .readerFor(new TypeReference<List<PreviousOrganisationCollectionItem>>() {
            }).readValue(prevOrgsNode);
        LocalDate localDate = LocalDate.now();

        assertAll(
            () -> assertThat(result.get(CHANGE_ORG_REQUEST_FIELD).toString(), is(emptyChangeOrgRequestField())),
            () -> assertThat(result.get(ORG_POLICY_1_FIELD).toString(),
                is(orgPolicyAsString(ORG_1_ID, ORG_1_NAME,
                    ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null))),
            () -> assertNotNull(previousOrganisations),
            () -> assertThat(previousOrganisations.size(), is(3)),
            () -> assertNotNull(previousOrganisations.get(0).getValue().getFromTimestamp()),
            () -> assertNotNull(previousOrganisations.get(0).getValue().getToTimestamp()),
            () -> assertNotNull(previousOrganisations.get(0).getValue().getOrganisationAddress()),
            () -> assertThat(previousOrganisations.get(0).getValue().getFromTimestamp().getDayOfMonth(), is(3)),
            () -> assertThat(previousOrganisations.get(0).getValue().getToTimestamp().getDayOfMonth(),
                is(localDate.getDayOfMonth()))
        );
    }

    private ContactInformation orgContactInformation() {
        return ContactInformation.builder()
            .addressLine1("Address 1")
            .addressLine2("Line 2")
            .addressLine3("Line 3")
            .country("UK")
            .county("London")
            .postCode("E6 1AB")
            .build();
    }

    private AddressUK organisationAddress() {
        return AddressUK.builder()
            .addressLine1("Address 1")
            .addressLine2("Line 2")
            .addressLine3("Line 3")
            .country("UK")
            .county("London")
            .postCode("E6 1AB")
            .build();
    }

    private PreviousOrganisation createPreviousOrganisation(LocalDate fromDate, LocalDate toDate) {
        return PreviousOrganisation.builder()
            .fromTimestamp(LocalDateTime.of(fromDate, LocalTime.now()))
            .toTimestamp(LocalDateTime.of(toDate, LocalTime.now()))
            .organisationName(ORG_2_NAME)
            .organisationAddress(organisationAddress())
            .build();
    }

    private ProfessionalUser prdUser(int id) {
        return new ProfessionalUser("UserId" + id, "fn" + id, "ln5" + id,
            String.format("User%sEmail", id), "active");
    }

    private String orgPolicyAsStringWithPreviousOrg(String organisationId,
                                                    String organisationName,
                                                    String orgPolicyReference,
                                                    String orgPolicyCaseAssignedRole,
                                                    List<PreviousOrganisationCollectionItem> previousOrganisations) {
        return String.format("{\"Organisation\":%s,\"OrgPolicyReference\":%s,\"OrgPolicyCaseAssignedRole\":%s, "
                             + "\"PreviousOrganisations\":%s}",
            organisationAsString(organisationId, organisationName),
            stringValueAsJson(orgPolicyReference),
            stringValueAsJson(orgPolicyCaseAssignedRole),
            listValueAsJson(previousOrganisations));
    }

    private String listValueAsJson(List<PreviousOrganisationCollectionItem> previousOrganisations) {
        try {
            return mapper.writeValueAsString(previousOrganisations);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    private String orgPolicyAsString(String organisationId,
                                     String organisationName,
                                     String orgPolicyReference,
                                     String orgPolicyCaseAssignedRole, String createdBy) {
        return String.format("{\"Organisation\":%s,\"OrgPolicyReference\":%s,\"OrgPolicyCaseAssignedRole\":%s,"
                             + "\"LastNoCRequestedBy\":%s}",
            organisationAsString(organisationId, organisationName),
            stringValueAsJson(orgPolicyReference), stringValueAsJson(orgPolicyCaseAssignedRole),
            stringValueAsJson(createdBy));
    }

    private String organisationAsString(String organisationId,
                                        String organisationName) {
        return String.format("{\"OrganisationID\":%s,\"OrganisationName\":%s}",
            stringValueAsJson(organisationId), stringValueAsJson(organisationName));
    }

    private String stringValueAsJson(String string) {
        return string == null ? "null" : String.format("\"%s\"", string);
    }

    private String emptyChangeOrgRequestField() {
        return "{\"Reason\":null,\"CaseRoleId\":null,\"NotesReason\":null,"
               + "\"ApprovalStatus\":null,\"RequestTimestamp\":null,\"OrganisationToAdd\":"
               + "{\"OrganisationID\":null,\"OrganisationName\":null},"
               + "\"OrganisationToRemove\":{\"OrganisationID\":null,\"OrganisationName\":null},"
               + "\"ApprovalRejectionTimestamp\":null,\"CreatedBy\":null}";
    }

    private String caseRoleIdField(String selectedCode) {
        return String.format("{\n"
                             + "\"value\":  {\n"
                             + "     \"code\": \"%s\",\n"
                             + "     \"label\": \"SomeLabel (Not used)\"\n"
                             + "},\n"
                             + "\"list_items\" : [\n"
                             + "     {\n"
                             + "         \"code\": \"[Defendant]\",\n"
                             + "         \"label\": \"Defendant\"\n"
                             + "     },\n"
                             + "     {\n"
                             + "         \"code\": \"[Claimant]\",\n"
                             + "         \"label\": \"Claimant\"\n"
                             + "     }\n"
                             + "]\n"
                             + "}\n", selectedCode);
    }

    private Map<String, JsonNode> createData(String organisationPolicy1,
                                             String organisationPolicy2,
                                             String organisationToAdd,
                                             String organisationToRemove) throws JsonProcessingException {
        return mapper.convertValue(mapper.readTree(String.format("{\n"
                                                                 + "    \"TextField\": \"TextFieldValue\",\n"
                                                                 + "    \"OrganisationPolicyField1\": %s,\n"
                                                                 + "    \"OrganisationPolicyField2\": %s,\n"
                                                                 + "    \"ChangeOrganisationRequestField\": {\n"
                                                                 + "        \"Reason\": null,\n"
                                                                 + "        \"CaseRoleId\": %s,\n"
                                                                 + "        \"NotesReason\": \"a\",\n"
                                                                 + "        \"ApprovalStatus\": 1,\n"
                                                                 + "        \"RequestTimestamp\": null,\n"
                                                                 + "        \"OrganisationToAdd\": %s,\n"
                                                                 + "        \"OrganisationToRemove\": %s,\n"
                                                                 + "        \"ApprovalRejectionTimestamp\": null,\n"
                                                                 + "        \"CreatedBy\": \"userEmail\"\n"
                                                                 + "    }\n"
                                                                 + "}", organisationPolicy1,
            organisationPolicy2, caseRoleIdField("[Claimant]"),
            organisationToAdd, organisationToRemove)), getHashMapTypeReference());
    }

    private Map<String, JsonNode> createData() throws JsonProcessingException {
        return createData(orgPolicyAsString(ORG_1_ID, ORG_1_NAME, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null),
            orgPolicyAsString(ORG_2_ID, ORG_2_NAME, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE, null),
            organisationAsString(null, null),
            organisationAsString(ORG_2_ID, ORG_2_NAME));
    }

    private Map<String, JsonNode> createSingleOrgData() throws JsonProcessingException {
        return createData(orgPolicyAsString(ORG_1_ID, ORG_1_NAME, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null),
            orgPolicyAsString(ORG_1_ID, ORG_1_NAME, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE, null),
            organisationAsString(null, null),
            organisationAsString(ORG_1_ID, ORG_1_NAME));
    }

    private Map<String, JsonNode> createPreviousOrgData(List<PreviousOrganisation> previousOrganisations)
        throws JsonProcessingException {

        List<PreviousOrganisationCollectionItem> previousOrganisationsCollection = previousOrganisations
            .stream()
            .map(org -> new PreviousOrganisationCollectionItem(UUID.randomUUID().toString(), org))
            .collect(Collectors.toList());

        return createData(orgPolicyAsString(ORG_1_ID, ORG_1_NAME, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null),
            orgPolicyAsStringWithPreviousOrg(ORG_2_ID, ORG_2_NAME,
                ORG_POLICY_2_REF,
                ORG_POLICY_2_ROLE,
                previousOrganisationsCollection),
            organisationAsString(null, null),
            organisationAsString(ORG_2_ID, ORG_2_NAME));
    }

    private Map<String, JsonNode> createAddOrgData()
        throws JsonProcessingException {
        return createData(orgPolicyAsString(ORG_1_ID, ORG_1_NAME, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE, null),
            orgPolicyAsString(ORG_2_ID, ORG_2_NAME,
                ORG_POLICY_2_REF,
                ORG_POLICY_2_ROLE, null),
            organisationAsString(ORG_2_ID, ORG_2_NAME),
            organisationAsString(null, null));
    }

    private TypeReference<HashMap<String, JsonNode>> getHashMapTypeReference() {
        return new TypeReference<HashMap<String, JsonNode>>() {
        };
    }
}
