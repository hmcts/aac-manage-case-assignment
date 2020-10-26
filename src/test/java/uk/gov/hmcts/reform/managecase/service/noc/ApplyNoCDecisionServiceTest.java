package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.managecase.api.payload.ApplyNoCDecisionRequest;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseUserRole;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.notify.EmailNotificationRequest;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.service.NotifyService;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import javax.validation.ValidationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_REQUEST_NOT_CONSIDERED;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_DATA_PROVIDED;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.UNKNOWN_NOC_APPROVAL_STATUS;
import static uk.gov.hmcts.reform.managecase.client.datastore.ApprovalStatus.NOT_CONSIDERED;
import static uk.gov.hmcts.reform.managecase.client.datastore.ApprovalStatus.REJECTED;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.APPROVAL_STATUS;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.CASE_ROLE_ID;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION_TO_ADD;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION_TO_REMOVE;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORG_POLICY_CASE_ASSIGNED_ROLE;

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
        MockitoAnnotations.initMocks(this);
        applyNoCDecisionService = new ApplyNoCDecisionService(prdRepository, dataStoreRepository,
            notifyService, new JacksonUtils(mapper), mapper);
    }

    @Test
    void shouldUpdateCaseDataWhenRemoveDecisionIsApplied() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createData())
            .reference(CASE_ID)
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
                    ORG_POLICY_1_REF, ORG_POLICY_1_ROLE))),
            () -> assertThat(result.get(ORG_POLICY_2_FIELD).toString(),
                is(orgPolicyAsString(null, null,
                    ORG_POLICY_2_REF, ORG_POLICY_2_ROLE)))
        );
    }

    @Test
    void shouldRemoveExistingOrgUsersWithAccessWhenRemoveDecisionIsApplied() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createData())
            .reference(CASE_ID)
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
    void shouldUpdateCaseDataWhenAddDecisionIsApplied() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createData(
                orgPolicyAsString(null, null, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE),
                orgPolicyAsString(null, null, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE),
                organisationAsString(ORG_3_ID, ORG_3_NAME),
                organisationAsString(null, null)
            ))
            .reference(CASE_ID)
            .build();

        when(prdRepository.findUsersByOrganisation(ORG_3_ID))
            .thenReturn(new FindUsersByOrganisationResponse(emptyList(), ORG_3_ID));

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        Map<String, JsonNode> result = applyNoCDecisionService.applyNoCDecision(request);

        assertAll(
            () -> assertThat(result.get(CHANGE_ORG_REQUEST_FIELD).toString(), is(emptyChangeOrgRequestField())),
            () -> assertThat(result.get(ORG_POLICY_1_FIELD).toString(),
                is(orgPolicyAsString(null, null,
                    ORG_POLICY_1_REF, ORG_POLICY_1_ROLE))),
            () -> assertThat(result.get(ORG_POLICY_2_FIELD).toString(),
                is(orgPolicyAsString(ORG_3_ID, ORG_3_NAME,
                    ORG_POLICY_2_REF, ORG_POLICY_2_ROLE)))
        );
    }

    @Test
    void shouldUpdateOrgUsersAccessWhenAddDecisionIsApplied() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createData(
                orgPolicyAsString(null, null, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE),
                orgPolicyAsString(null, null, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE),
                organisationAsString(ORG_3_ID, ORG_3_NAME),
                organisationAsString(null, null)
            ))
            .reference(CASE_ID)
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
                orgPolicyAsString(ORG_1_ID, ORG_1_NAME, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE),
                orgPolicyAsString(ORG_2_ID, ORG_2_NAME, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE),
                organisationAsString(ORG_3_ID, ORG_3_NAME),
                organisationAsString(ORG_2_ID, ORG_2_NAME)
            ))
            .reference(CASE_ID)
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
                    ORG_POLICY_1_REF, ORG_POLICY_1_ROLE))),
            () -> assertThat(result.get(ORG_POLICY_2_FIELD).toString(),
                is(orgPolicyAsString(ORG_3_ID, ORG_3_NAME,
                    ORG_POLICY_2_REF, ORG_POLICY_2_ROLE)))
        );
    }

    @Test
    void shouldUpdateOrgUsersAccessWhenReplaceDecisionIsApplied() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
            .data(createData(
                orgPolicyAsString(ORG_1_ID, ORG_1_NAME, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE),
                orgPolicyAsString(ORG_2_ID, ORG_2_NAME, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE),
                organisationAsString(ORG_3_ID, ORG_3_NAME),
                organisationAsString(ORG_2_ID, ORG_2_NAME)
            ))
            .reference(CASE_ID)
            .build();

        List<CaseUserRole> existingCaseAssignments = List.of(
            new CaseUserRole(CASE_ID, USER_ID_1, ORG_POLICY_1_ROLE),
            new CaseUserRole(CASE_ID, USER_ID_2, ORG_POLICY_1_ROLE),
            new CaseUserRole(CASE_ID, USER_ID_3, ORG_POLICY_1_ROLE)
        );
        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null))
            .thenReturn(existingCaseAssignments);

        FindUsersByOrganisationResponse usersByOrganisation1 = new FindUsersByOrganisationResponse(List.of(
            prdUser(2), prdUser(4), prdUser(4)), ORG_2_ID);
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
            () -> assertThat(caseUserRolesCaptor.getValue().get(0).getCaseRole(), is(ORG_POLICY_1_ROLE)),
            () -> verify(dataStoreRepository, never()).removeCaseUserRoles(Mockito.any(), Mockito.eq(ORG_1_ID)),
            () -> verify(dataStoreRepository, never()).removeCaseUserRoles(Mockito.any(), Mockito.eq(ORG_3_ID)),
            () -> verify(notifyService).sendEmail(emailRequestsCaptor.capture()),
            () -> assertThat(emailRequestsCaptor.getValue().size(), is(1)),
            () -> assertThat(emailRequestsCaptor.getValue().get(0).getCaseId(), is(CASE_ID)),
            () -> assertThat(emailRequestsCaptor.getValue().get(0).getEmailAddress(), is("User2Email")),
            () -> verify(dataStoreRepository).assignCase(caseRolesCaptor.capture(), Mockito.eq(CASE_ID),
                Mockito.eq("UserId3"), Mockito.eq(ORG_3_ID)),
            () -> assertThat(caseRolesCaptor.getValue().size(), is(1)),
            () -> assertThat(caseRolesCaptor.getValue().get(0), is(ORG_POLICY_2_ROLE))
        );
    }

    @Test
    void shouldUpdateCaseDataWhenRejectedDecisionIsApplied() throws JsonProcessingException {
        Map<String, JsonNode> data = createData();
        ((ObjectNode) data.get(CHANGE_ORG_REQUEST_FIELD)).set(APPROVAL_STATUS, new TextNode(REJECTED.getCode()) {
        });
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .reference(CASE_ID)
            .build();

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        Map<String, JsonNode> result = applyNoCDecisionService.applyNoCDecision(request);

        assertAll(
            () -> assertThat(result.get(CHANGE_ORG_REQUEST_FIELD).toString(), is(emptyChangeOrgRequestField())),
            () -> assertThat(result.get(ORG_POLICY_1_FIELD).toString(),
                is(orgPolicyAsString(ORG_1_ID, ORG_1_NAME,
                    ORG_POLICY_1_REF, ORG_POLICY_1_ROLE))),
            () -> assertThat(result.get(ORG_POLICY_2_FIELD).toString(),
                is(orgPolicyAsString(ORG_2_ID, ORG_2_NAME,
                    ORG_POLICY_2_REF, ORG_POLICY_2_ROLE)))
        );
    }

    @Test
    void shouldErrorWhenApprovalStatusIsNotSet() throws JsonProcessingException {
        Map<String, JsonNode> data = createData();
        ((ObjectNode) data.get(CHANGE_ORG_REQUEST_FIELD)).set(APPROVAL_STATUS, mapper.nullNode());
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .reference(CASE_ID)
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
        ((ObjectNode) data.get(CHANGE_ORG_REQUEST_FIELD)).set(APPROVAL_STATUS, new TextNode(NOT_CONSIDERED.getCode()));
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .reference(CASE_ID)
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
            .reference(CASE_ID)
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
            .reference(CASE_ID)
            .build();

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> applyNoCDecisionService.applyNoCDecision(request));

        assertAll(
            () -> assertThat(exception.getMessage(), is("A value is expected for 'CaseRoleId'"))
        );
    }

    @Test
    void shouldErrorWhenNoOrgPolicyExistsForCaseRoleId() throws JsonProcessingException {
        Map<String, JsonNode> data = createData();
        ((ObjectNode) data.get(CHANGE_ORG_REQUEST_FIELD)).set(CASE_ROLE_ID, new TextNode("UnknownCaseRoleId"));
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .reference(CASE_ID)
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
            .reference(CASE_ID)
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
    void shouldErrorWhenOrganisationToAddIsNotPresent() throws JsonProcessingException {
        Map<String, JsonNode> data = createData();
        ((ObjectNode) data.get(CHANGE_ORG_REQUEST_FIELD)).remove(ORGANISATION_TO_ADD);
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .reference(CASE_ID)
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
    void shouldErrorWhenOrganisationToRemoveIsNotPresent() throws JsonProcessingException {
        Map<String, JsonNode> data = createData();
        ((ObjectNode) data.get(CHANGE_ORG_REQUEST_FIELD)).remove(ORGANISATION_TO_REMOVE);
        CaseDetails caseDetails = CaseDetails.builder()
            .data(data)
            .reference(CASE_ID)
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
            .reference(CASE_ID)
            .build();

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> applyNoCDecisionService.applyNoCDecision(request));

        assertAll(
            () -> assertThat(exception.getMessage(), is(NO_DATA_PROVIDED))
        );
    }

    private ProfessionalUser prdUser(int id) {
        return new ProfessionalUser("UserId" + id, "fn" + id, "ln5" + id, String.format("User%sEmail", id), "active");
    }

    private String orgPolicyAsString(String organisationId,
                                     String organisationName,
                                     String orgPolicyReference,
                                     String orgPolicyCaseAssignedRole) {
        return String.format("{\"Organisation\":%s,\"OrgPolicyReference\":%s,\"OrgPolicyCaseAssignedRole\":%s}",
            organisationAsString(organisationId, organisationName),
            stringValueAsJson(orgPolicyReference), stringValueAsJson(orgPolicyCaseAssignedRole));
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
            + "{\"OrganisationID\":null,\"OrganisationName\":null},\"OrganisationToRemove\":"
            + "{\"OrganisationID\":null,\"OrganisationName\":null},\"ApprovalRejectionTimestamp\":null}";
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
                + "        \"CaseRoleId\": \"[Claimant]\",\n"
                + "        \"NotesReason\": \"a\",\n"
                + "        \"ApprovalStatus\": 1,\n"
                + "        \"RequestTimestamp\": null,\n"
                + "        \"OrganisationToAdd\": %s,\n"
                + "        \"OrganisationToRemove\": %s,\n"
                + "        \"ApprovalRejectionTimestamp\": null\n"
                + "    }\n"
                + "}", organisationPolicy1, organisationPolicy2, organisationToAdd, organisationToRemove)),
            getHashMapTypeReference());
    }

    private Map<String, JsonNode> createData() throws JsonProcessingException {
        return createData(orgPolicyAsString(ORG_1_ID, ORG_1_NAME, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE),
            orgPolicyAsString(ORG_2_ID, ORG_2_NAME, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE),
            organisationAsString(null, null),
            organisationAsString(ORG_2_ID, ORG_2_NAME));
    }

    private TypeReference<HashMap<String, JsonNode>> getHashMapTypeReference() {
        return new TypeReference<HashMap<String, JsonNode>>() {
        };
    }
}