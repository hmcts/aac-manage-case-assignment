package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.is;

class ApplyNoCDecisionServiceTest {

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

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String CASE_ID = "123";
    private static final String ORG_POLICY_1_ID = "OrgPolicy1Id";
    private static final String ORG_POLICY_2_ID = "OrgPolicy2Id";
    private static final String ORG_POLICY_1_NAME = "OrgPolicy1Name";
    private static final String ORG_POLICY_2_NAME = "OrgPolicy2Name";
    private static final String ORG_POLICY_1_REF = "DefendantPolicy";
    private static final String ORG_POLICY_2_REF = "ClaimantPolicy";
    private static final String ORG_POLICY_1_ROLE = "[Defendant]";
    private static final String ORG_POLICY_2_ROLE = "[Claimant]";
    private static final String CHANGE_ORG_REQUEST_FIELD = "ChangeOrganisationRequestField";
    private static final String ORG_POLICY_1_FIELD = "OrganisationPolicyField1";
    private static final String ORG_POLICY_2_FIELD = "OrganisationPolicyField2";
    private static final String OTHER_ORG_ID = "Org3Id";
    private static final String OTHER_ORG_NAME = "Org3Name";

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
        when(prdRepository.findUsersByOrganisation(ORG_POLICY_2_ID))
                .thenReturn(new FindUsersByOrganisationResponse(emptyList(), ORG_POLICY_2_ID));

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        Map<String, JsonNode> result = applyNoCDecisionService.applyNoCDecision(request);

        assertAll(
                () -> assertThat(result.get(CHANGE_ORG_REQUEST_FIELD).toString(), is(emptyChangeOrgRequestField())),
                () -> assertThat(result.get(ORG_POLICY_1_FIELD).toString(),
                        is(orgPolicyAsString(ORG_POLICY_1_ID, ORG_POLICY_1_NAME,
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
                new CaseUserRole(CASE_ID, "UserId1", ORG_POLICY_2_ROLE),
                new CaseUserRole(CASE_ID, "UserId2", ORG_POLICY_2_ROLE),
                new CaseUserRole(CASE_ID, "UserId3", ORG_POLICY_2_ROLE)
        );
        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null))
                .thenReturn(existingCaseAssignments);

        FindUsersByOrganisationResponse usersByOrganisation = new FindUsersByOrganisationResponse(List.of(
                new ProfessionalUser("UserId1", "fn1", "ln1", "User1Email", "active"),
                new ProfessionalUser("UserId3", "fn3", "ln3", "User3Email", "active"),
                new ProfessionalUser("UserId4", "fn4", "ln4", "User4Email", "active")
        ), ORG_POLICY_2_ID);
        when(prdRepository.findUsersByOrganisation(ORG_POLICY_2_ID)).thenReturn(usersByOrganisation);

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        Map<String, JsonNode> result = applyNoCDecisionService.applyNoCDecision(request);

        assertAll(
                () -> verify(dataStoreRepository).removeCaseUserRoles(caseUserRolesCaptor.capture(), Mockito.eq(ORG_POLICY_2_ID)),
                () -> assertThat(caseUserRolesCaptor.getValue().size(), is(2)),
                () -> assertThat(caseUserRolesCaptor.getValue().get(0).getCaseId(), is(CASE_ID)),
                () -> assertThat(caseUserRolesCaptor.getValue().get(0).getUserId(), is("UserId1")),
                () -> assertThat(caseUserRolesCaptor.getValue().get(0).getCaseRole(), is(ORG_POLICY_2_ROLE)),
                () -> assertThat(caseUserRolesCaptor.getValue().get(1).getCaseId(), is(CASE_ID)),
                () -> assertThat(caseUserRolesCaptor.getValue().get(1).getUserId(), is("UserId3")),
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
    void shouldUpdateCaseDataWhenReplaceDecisionIsApplied() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
                .data(createData(
                        orgPolicyAsString(ORG_POLICY_1_ID, ORG_POLICY_1_NAME, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE),
                        orgPolicyAsString(ORG_POLICY_2_ID, ORG_POLICY_2_NAME, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE),
                        organisationAsString(OTHER_ORG_ID, OTHER_ORG_NAME),
                        organisationAsString(ORG_POLICY_2_ID, ORG_POLICY_2_NAME)
                ))
                .reference(CASE_ID)
                .build();

        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null)).thenReturn(emptyList());
        when(prdRepository.findUsersByOrganisation(ORG_POLICY_2_ID))
                .thenReturn(new FindUsersByOrganisationResponse(emptyList(), ORG_POLICY_2_ID));
        when(prdRepository.findUsersByOrganisation(OTHER_ORG_ID))
                .thenReturn(new FindUsersByOrganisationResponse(emptyList(), OTHER_ORG_ID));

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        Map<String, JsonNode> result = applyNoCDecisionService.applyNoCDecision(request);

        assertAll(
                () -> assertThat(result.get(CHANGE_ORG_REQUEST_FIELD).toString(), is(emptyChangeOrgRequestField())),
                () -> assertThat(result.get(ORG_POLICY_1_FIELD).toString(),
                        is(orgPolicyAsString(ORG_POLICY_1_ID, ORG_POLICY_1_NAME,
                                ORG_POLICY_1_REF, ORG_POLICY_1_ROLE))),
                () -> assertThat(result.get(ORG_POLICY_2_FIELD).toString(),
                        is(orgPolicyAsString(OTHER_ORG_ID, OTHER_ORG_NAME,
                                ORG_POLICY_2_REF, ORG_POLICY_2_ROLE)))
        );
    }

    @Test
    void shouldUpdateOrgUsersAccessWhenReplaceDecisionIsApplied() throws JsonProcessingException {
        CaseDetails caseDetails = CaseDetails.builder()
                .data(createData(
                        orgPolicyAsString(ORG_POLICY_1_ID, ORG_POLICY_1_NAME, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE),
                        orgPolicyAsString(ORG_POLICY_2_ID, ORG_POLICY_2_NAME, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE),
                        organisationAsString(OTHER_ORG_ID, OTHER_ORG_NAME),
                        organisationAsString(ORG_POLICY_2_ID, ORG_POLICY_2_NAME)
                ))
                .reference(CASE_ID)
                .build();

        List<CaseUserRole> existingCaseAssignments = List.of(
                new CaseUserRole(CASE_ID, "UserId1", ORG_POLICY_2_ROLE),
                new CaseUserRole(CASE_ID, "UserId2", ORG_POLICY_2_ROLE),
                new CaseUserRole(CASE_ID, "UserId3", ORG_POLICY_2_ROLE)
        );
        when(dataStoreRepository.getCaseAssignments(singletonList(CASE_ID), null))
                .thenReturn(existingCaseAssignments);

        FindUsersByOrganisationResponse usersByOrganisation1 = new FindUsersByOrganisationResponse(List.of(
                new ProfessionalUser("UserId1", "fn1", "ln1", "User1Email", "active"),
                new ProfessionalUser("UserId3", "fn3", "ln3", "User3Email", "active"),
                new ProfessionalUser("UserId4", "fn4", "ln4", "User4Email", "active")
        ), ORG_POLICY_2_ID);
        when(prdRepository.findUsersByOrganisation(ORG_POLICY_2_ID)).thenReturn(usersByOrganisation1);

        FindUsersByOrganisationResponse usersByOrganisation2 = new FindUsersByOrganisationResponse(List.of(
                new ProfessionalUser("UserId5", "fn5", "ln5", "User5Email", "active"),
                new ProfessionalUser("UserId6", "fn6", "ln6", "User6Email", "active")
        ), OTHER_ORG_ID);
        when(prdRepository.findUsersByOrganisation(OTHER_ORG_ID)).thenReturn(usersByOrganisation2);

        ApplyNoCDecisionRequest request = new ApplyNoCDecisionRequest(caseDetails);

        Map<String, JsonNode> result = applyNoCDecisionService.applyNoCDecision(request);

        assertAll(
                () -> verify(dataStoreRepository).removeCaseUserRoles(caseUserRolesCaptor.capture(), Mockito.eq(ORG_POLICY_2_ID)),
                () -> assertThat(caseUserRolesCaptor.getValue().size(), is(2)),
                () -> assertThat(caseUserRolesCaptor.getValue().get(0).getCaseId(), is(CASE_ID)),
                () -> assertThat(caseUserRolesCaptor.getValue().get(0).getUserId(), is("UserId1")),
                () -> assertThat(caseUserRolesCaptor.getValue().get(0).getCaseRole(), is(ORG_POLICY_2_ROLE)),
                () -> assertThat(caseUserRolesCaptor.getValue().get(1).getCaseId(), is(CASE_ID)),
                () -> assertThat(caseUserRolesCaptor.getValue().get(1).getUserId(), is("UserId3")),
                () -> assertThat(caseUserRolesCaptor.getValue().get(1).getCaseRole(), is(ORG_POLICY_2_ROLE)),
                () -> verify(notifyService).sendEmail(emailRequestsCaptor.capture()),
                () -> assertThat(emailRequestsCaptor.getValue().size(), is(2)),
                () -> assertThat(emailRequestsCaptor.getValue().get(0).getCaseId(), is(CASE_ID)),
                () -> assertThat(emailRequestsCaptor.getValue().get(0).getEmailAddress(), is("User1Email")),
                () -> assertThat(emailRequestsCaptor.getValue().get(1).getCaseId(), is(CASE_ID)),
                () -> assertThat(emailRequestsCaptor.getValue().get(1).getEmailAddress(), is("User3Email")),
                () -> verify(dataStoreRepository, times(2))
                        .assignCase(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()),
                () -> verify(dataStoreRepository).assignCase(Mockito.any(), Mockito.eq(CASE_ID),
                        Mockito.eq("UserId5"), Mockito.eq(OTHER_ORG_ID)),
                () -> verify(dataStoreRepository).assignCase(Mockito.any(), Mockito.eq(CASE_ID),
                        Mockito.eq("UserId6"), Mockito.eq(OTHER_ORG_ID)),
                () -> assertThat(1, is(1))
        );
    }

    private String orgPolicyAsString(String organisationId,
                                     String organisationName,
                                     String orgPolicyReference,
                                     String orgPolicyCaseAssignedRole) {
        return String.format("{\"Organisation\":%s," +
                        "\"OrgPolicyReference\":%s,\"OrgPolicyCaseAssignedRole\":%s}",
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
        return "{\"Reason\":null,\"CaseRoleId\":null,\"NotesReason\":null," +
                "\"ApprovalStatus\":null,\"RequestTimestamp\":null,\"OrganisationToAdd\":" +
                "{\"OrganisationID\":null,\"OrganisationName\":null},\"OrganisationToRemove\":" +
                "{\"OrganisationID\":null,\"OrganisationName\":null},\"ApprovalRejectionTimestamp\":null}";
    }

    private Map<String, JsonNode> createData(String organisationPolicy1,
                                             String organisationPolicy2,
                                             String organisationToAdd,
                                             String organisationToRemove) throws JsonProcessingException {
        return mapper.convertValue(mapper.readTree(String.format("{\n" +
                "    \"TextField\": \"TextFieldValue\",\n" +
                "    \"OrganisationPolicyField1\": %s,\n" +
                "    \"OrganisationPolicyField2\": %s,\n" +
                "    \"ChangeOrganisationRequestField\": {\n" +
                "        \"Reason\": null,\n" +
                "        \"CaseRoleId\": \"[Claimant]\",\n" +
                "        \"NotesReason\": \"a\",\n" +
                "        \"ApprovalStatus\": \"Approved\",\n" +
                "        \"RequestTimestamp\": null,\n" +
                "        \"OrganisationToAdd\": %s,\n" +
                "        \"OrganisationToRemove\": %s,\n" +
                "        \"ApprovalRejectionTimestamp\": null\n" +
                "    }\n" +
                "}", organisationPolicy1, organisationPolicy2, organisationToAdd, organisationToRemove)),
                getHashMapTypeReference());
    }

    private Map<String, JsonNode> createData() throws JsonProcessingException {
        return createData(orgPolicyAsString(ORG_POLICY_1_ID, ORG_POLICY_1_NAME, ORG_POLICY_1_REF, ORG_POLICY_1_ROLE),
                orgPolicyAsString(ORG_POLICY_2_ID, ORG_POLICY_2_NAME, ORG_POLICY_2_REF, ORG_POLICY_2_ROLE),
                organisationAsString(null, null),
                organisationAsString(ORG_POLICY_2_ID, ORG_POLICY_2_NAME));
    }

    private TypeReference<HashMap<String, JsonNode>> getHashMapTypeReference() {
        return new TypeReference<HashMap<String, JsonNode>>() {
        };
    }
}