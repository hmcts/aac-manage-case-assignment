package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCException;
import uk.gov.hmcts.reform.managecase.api.payload.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.reform.managecase.api.payload.IdamUser;
import uk.gov.hmcts.reform.managecase.api.payload.RequestNoticeOfChangeResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewActionableEvent;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewResource;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewType;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.data.user.UserRepository;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.DynamicList;
import uk.gov.hmcts.reform.managecase.domain.DynamicListElement;
import uk.gov.hmcts.reform.managecase.domain.NoCRequestDetails;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DataStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.noc.NoCValidationError.INVALID_CASE_ROLE_FIELD;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.APPROVED;
import static uk.gov.hmcts.reform.managecase.domain.ApprovalStatus.PENDING;

@SuppressWarnings({"PMD.UseConcurrentHashMap",
    "PMD.AvoidDuplicateLiterals",
    "PMD.ExcessiveImports",
    "PMD.TooManyMethods",
    "PMD.DataflowAnomalyAnalysis"})
class RequestNoticeOfChangeServiceTest {

    private static final String CASE_ID = "1567934206391385";
    private static final String INCUMBENT_ORGANISATION_ID = "INCUMBENT_ORG_ID_1";
    private static final String CASE_ASSIGNED_ROLE = "CASE_ASSIGNED_ROLE";
    private static final String NOC_REQUEST_EVENT = "NocRequest";
    private static final String INVOKERS_ORGANISATION_IDENTIFIER = "PRD_ORG_IDENTIFIER";
    private static final String ORG_POLICY_REFERENCE = "orgPolicyReference";
    private static final String CHANGE_ORGANISATION_REQUEST_KEY = "ChangeOrganisationRequest";
    private static final String ORGANISATION_POLICY_KEY = "OrganisationPolicy";
    private static final String JURISDICTION_ONE = "JURISDICTION_1";
    private static final String SOLICITOR_ROLE = "caseworker-" + JURISDICTION_ONE + "-solicitor";
    private static final String NON_SOLICITOR_ROLE = "caseworker-" + JURISDICTION_ONE + "-citizen";
    private static final String USER_INFO_UID = "userInfoUid";
    private static final String DYNAMIC_LIST_LABEL = "name";

    private NoCRequestDetails noCRequestDetails;
    private Organisation incumbentOrganisation;
    private CaseDetails caseDetails;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RequestNoticeOfChangeService service;

    @Mock
    private UserRepository userRepository;
    @Mock
    private DataStoreRepository dataStoreRepository;
    @Mock
    private DefinitionStoreRepository definitionStoreRepository;
    @Mock
    private PrdRepository prdRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private JacksonUtils jacksonUtils;

    @BeforeEach
    void setUp() {
        openMocks(this);
        FindUsersByOrganisationResponse findUsersByOrganisationResponse = new FindUsersByOrganisationResponse(
            emptyList(), INVOKERS_ORGANISATION_IDENTIFIER);
        given(prdRepository.findUsersByOrganisation()).willReturn(findUsersByOrganisationResponse);

        CaseViewType caseViewType = new CaseViewType();
        caseViewType.setId("id");
        caseViewType.setName(DYNAMIC_LIST_LABEL);
        CaseViewActionableEvent caseViewActionableEvent = new CaseViewActionableEvent();
        caseViewActionableEvent.setId(NOC_REQUEST_EVENT);
        CaseViewActionableEvent[] caseViewActionableEvents = {caseViewActionableEvent};
        CaseViewResource caseViewResource = new CaseViewResource();
        caseViewResource.setReference(CASE_ID);
        caseViewResource.setCaseViewActionableEvents(caseViewActionableEvents);
        caseViewResource.setCaseType(caseViewType);
        incumbentOrganisation = Organisation.builder().organisationID(INCUMBENT_ORGANISATION_ID).build();
        OrganisationPolicy organisationPolicy = new OrganisationPolicy(incumbentOrganisation,
                                                                       ORG_POLICY_REFERENCE, CASE_ASSIGNED_ROLE,
                                                                       Lists.newArrayList(), null);

        noCRequestDetails = NoCRequestDetails.builder()
            .caseViewResource(caseViewResource)
            .organisationPolicy(organisationPolicy)
            .build();

        caseDetails = CaseDetails.builder().id(CASE_ID).data(new HashMap<>()).build();

        given(dataStoreRepository.findCaseByCaseIdAsSystemUserUsingExternalApi(CASE_ID)).willReturn(caseDetails);

        IdamUser idamUser = new IdamUser();
        idamUser.setEmail("test@test.com");
        given(userRepository.getUser()).willReturn(idamUser);

        List<CaseRole> caseRoles = List.of(CaseRole.builder().id(CASE_ASSIGNED_ROLE).name(DYNAMIC_LIST_LABEL).build());
        given(definitionStoreRepository.caseRoles(anyString(), anyString(), anyString())).willReturn(caseRoles);
    }

    @Test
    @DisplayName("Generate a Notice Of Change Request with an Incumbent Organisation")
    void testGenerateNocRequestWithIncumbentOrganisation() {
        service.requestNoticeOfChange(noCRequestDetails);

        assertSubmitEventForCaseParameters(incumbentOrganisation);
    }

    @Test
    @DisplayName("Generate a Notice Of Change Request with no Incumbent Organisation")
    void testGenerateNocRequestWithOutIncumbentOrganisation() {
        noCRequestDetails.setOrganisationPolicy(new OrganisationPolicy(null,
                                                                       ORG_POLICY_REFERENCE,
                                                                       CASE_ASSIGNED_ROLE, Lists.newArrayList(), null));
        final Organisation nullIncumbentOrganisation = null;
        service.requestNoticeOfChange(noCRequestDetails);

        assertSubmitEventForCaseParameters(nullIncumbentOrganisation);
    }

    private void assertSubmitEventForCaseParameters(Organisation organisationToRemove) {
        ArgumentCaptor<String> caseIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> eventIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ChangeOrganisationRequest> corArgumentCaptor
            = ArgumentCaptor.forClass(ChangeOrganisationRequest.class);

        verify(dataStoreRepository).submitNoticeOfChangeRequestEvent(caseIdCaptor.capture(),
                                                                     eventIdCaptor.capture(),
                                                                     corArgumentCaptor.capture());

        assertThat(caseIdCaptor.getValue()).isEqualTo(CASE_ID);
        assertThat(eventIdCaptor.getValue()).isEqualTo(NOC_REQUEST_EVENT);

        DynamicListElement dynamicListElement = DynamicListElement.builder()
            .code(CASE_ASSIGNED_ROLE)
            .label(DYNAMIC_LIST_LABEL)
            .build();
        DynamicList dynamicList = DynamicList.builder()
            .value(dynamicListElement)
            .listItems(List.of(dynamicListElement))
            .build();
        ChangeOrganisationRequest capturedCor = corArgumentCaptor.getValue();
        assertAll(
            () -> assertThat(capturedCor.getCaseRoleId()).isEqualTo(dynamicList),
            () -> assertThat(capturedCor.getOrganisationToAdd().getOrganisationID()).isEqualTo(
                INVOKERS_ORGANISATION_IDENTIFIER),
            () -> assertThat(capturedCor.getOrganisationToRemove()).isEqualTo(organisationToRemove),
            () -> assertThat(capturedCor.getRequestTimestamp()).isNotNull()
        );
    }

    @Test
    @DisplayName("NoC auto approval with no Change Organisation Request returns PENDING state")
    void testAutoApprovalWithCorNotPresent() {
        RequestNoticeOfChangeResponse requestNoticeOfChangeResponse
            = service.requestNoticeOfChange(noCRequestDetails);


        assertThat(requestNoticeOfChangeResponse.getApprovalStatus()).isEqualTo(PENDING);
    }

    @Test
    @DisplayName("NoC auto approval with Change Organisation Request and Case Role present returns PENDING state")
    void testAutoApprovalCorPresentCaseRolePresent() {
        ChangeOrganisationRequest changeOrganisationRequest = ChangeOrganisationRequest.builder()
            .caseRoleId(DynamicList.builder().build())
            .build();
        updateCaseDetailsData(caseDetails, CHANGE_ORGANISATION_REQUEST_KEY, changeOrganisationRequest);

        given(jacksonUtils.convertValue(any(), any())).willReturn(changeOrganisationRequest);

        RequestNoticeOfChangeResponse requestNoticeOfChangeResponse =
            service.requestNoticeOfChange(noCRequestDetails);

        verify(dataStoreRepository, never())
            .assignCase(any(), any(String.class), any(String.class), any(String.class));
        assertThat(requestNoticeOfChangeResponse.getApprovalStatus()).isEqualTo(PENDING);
    }

    @Test
    @DisplayName("NoC auto approval with Change Organisation Request and no Case Role present and no assigned "
        + "Organisation Policies returns PENDING state")
    void testAutoApprovalCorPresentBlankCaseRoleOrgPoliciesNotAssigned() {
        ChangeOrganisationRequest changeOrganisationRequest = ChangeOrganisationRequest.builder().build();
        updateCaseDetailsData(caseDetails, CHANGE_ORGANISATION_REQUEST_KEY, changeOrganisationRequest);

        List<OrganisationPolicy> organisationPolicies = updateCaseDetailsWithOrganisationPolicies(caseDetails);

        given(jacksonUtils.convertValue(any(JsonNode.class), any()))
            .willReturn(changeOrganisationRequest)
            .willReturn(organisationPolicies.get(0))
            .willReturn(organisationPolicies.get(1))
            .willReturn(organisationPolicies.get(2));

        RequestNoticeOfChangeResponse requestNoticeOfChangeResponse
            = service.requestNoticeOfChange(noCRequestDetails);

        assertThat(requestNoticeOfChangeResponse.getApprovalStatus()).isEqualTo(PENDING);
    }

    @Test
    @DisplayName("NoC auto approval with Change Organisation Request and no Case Role present and assigned "
        + "Organisation Policies returns APPROVED state")
    void testAutoApprovalCorPresentBlankCaseRoleOrgPoliciesAssigned() {
        nocAutoApprovedByAdminOrSolicitor(false);

        RequestNoticeOfChangeResponse requestNoticeOfChangeResponse
            = service.requestNoticeOfChange(noCRequestDetails);

        verify(dataStoreRepository, never())
            .assignCase(any(), any(String.class), any(String.class), any(String.class));
        assertThat(requestNoticeOfChangeResponse.getApprovalStatus()).isEqualTo(APPROVED);
    }

    private void nocAutoApprovedByAdminOrSolicitor(boolean isAdminOrSolicitor) {
        ChangeOrganisationRequest changeOrganisationRequest = ChangeOrganisationRequest.builder().build();
        updateCaseDetailsData(caseDetails, CHANGE_ORGANISATION_REQUEST_KEY, changeOrganisationRequest);

        List<OrganisationPolicy> organisationPolicies = updateCaseDetailsWithOrganisationPolicies(caseDetails);

        Organisation invokersOrganisation =
            Organisation.builder().organisationID(INVOKERS_ORGANISATION_IDENTIFIER).build();
        OrganisationPolicy invokersOrganisationPolicy = new OrganisationPolicy(invokersOrganisation,
                                                                               ORG_POLICY_REFERENCE,
                                                                               CASE_ASSIGNED_ROLE,
                                                                               Lists.newArrayList(), null);

        updateCaseDetailsData(caseDetails, ORGANISATION_POLICY_KEY, invokersOrganisationPolicy);

        setInvokerToActAsAnAdminOrSolicitor(isAdminOrSolicitor);

        given(jacksonUtils.convertValue(any(JsonNode.class), any()))
            .willReturn(changeOrganisationRequest)
            .willReturn(organisationPolicies.get(0))
            .willReturn(organisationPolicies.get(1))
            .willReturn(organisationPolicies.get(2))
            .willReturn(invokersOrganisationPolicy);
    }

    @Test
    @DisplayName("Generate a Notice Of Change Request with approval and Auto assignment of case roles")
    void testApprovalComplete() {
        caseDetails.setJurisdiction(JURISDICTION_ONE);
        nocAutoApprovedByAdminOrSolicitor(true);

        RequestNoticeOfChangeResponse requestNoticeOfChangeResponse
            = service.requestNoticeOfChange(noCRequestDetails);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> caseRolesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> caseIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> organisationIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(dataStoreRepository).assignCase(caseRolesCaptor.capture(),
                                               caseIdCaptor.capture(),
                                               userIdCaptor.capture(),
                                               organisationIdCaptor.capture());

        assertThat(caseRolesCaptor.getValue()).isNotEmpty();
        assertThat(caseIdCaptor.getValue()).isEqualTo(CASE_ID);
        assertThat(userIdCaptor.getValue()).isEqualTo(USER_INFO_UID);
        assertThat(organisationIdCaptor.getValue()).isEqualTo(INVOKERS_ORGANISATION_IDENTIFIER);
        assertThat(requestNoticeOfChangeResponse.getApprovalStatus()).isEqualTo(APPROVED);
    }

    @Test
    @DisplayName("Generate a Notice Of Change Request with approval but no Auto assignment of case roles")
    void testNotActingAsSolicitor() {
        caseDetails.setJurisdiction(JURISDICTION_ONE);
        nocAutoApprovedByAdminOrSolicitor(false);

        RequestNoticeOfChangeResponse requestNoticeOfChangeResponse
            = service.requestNoticeOfChange(noCRequestDetails);

        verify(dataStoreRepository, never())
            .assignCase(any(), any(String.class), any(String.class), any(String.class));
        assertThat(requestNoticeOfChangeResponse.getApprovalStatus()).isEqualTo(APPROVED);
    }

    @Test
    @DisplayName("Generate a Notice Of Change Request throws Validation Exception if caseRoleId not "
        + "present in definition store")
    void testThrowsValidationErrorIfCaseRoleIdNotPresentInDefinitionStore() {
        given(definitionStoreRepository.caseRoles(anyString(), anyString(), anyString())).willReturn(emptyList());
        NoCException exception = assertThrows(
            NoCException.class,
            () -> service.requestNoticeOfChange(noCRequestDetails)
        );

        assertThat(exception.getErrorCode()).isEqualTo("missing-cor-case-role-id");

        assertThat(exception.getErrorMessage()).isEqualTo(
            String.format("Missing ChangeOrganisationRequest.CaseRoleID %s in the case definition",
                          CASE_ASSIGNED_ROLE));
    }

    private void setInvokerToActAsAnAdminOrSolicitor(boolean actAsAnAdminOrSolicitor) {
        List<String> roles = actAsAnAdminOrSolicitor ? List.of(SOLICITOR_ROLE) : List.of(NON_SOLICITOR_ROLE);
        UserInfo userInfo = new UserInfo("sub",
                                         USER_INFO_UID,
                                         "name",
                                         "givenName",
                                         "familyName",
                                         roles);
        given(securityUtils.getUserInfo()).willReturn(userInfo);
        given(securityUtils.hasSolicitorAndJurisdictionRoles(any(), anyString())).willReturn(actAsAnAdminOrSolicitor);
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private List<OrganisationPolicy> updateCaseDetailsWithOrganisationPolicies(CaseDetails caseDetails) {
        List<OrganisationPolicy> organisationPolicies = new ArrayList<>();
        for (int loopCounter = 0; loopCounter < 3; loopCounter++) {
            Organisation organisation = Organisation.builder().organisationID("OrganisationId").build();
            OrganisationPolicy organisationPolicy =
                new OrganisationPolicy(organisation, ORG_POLICY_REFERENCE,
                                       "CaseRole" + loopCounter, Lists.newArrayList(), null);
            organisationPolicies.add(organisationPolicy);
            updateCaseDetailsData(caseDetails, "OrganisationPolicy" + loopCounter, organisationPolicy);
        }

        return organisationPolicies;
    }

    private void updateCaseDetailsData(CaseDetails caseDetails,
                                       String dataKey,
                                       Object changeOrganisationRequest) {
        Map<String, JsonNode> data = caseDetails.getData();
        data.put(dataKey, objectMapper.convertValue(changeOrganisationRequest, JsonNode.class));

        caseDetails.setData(data);
    }

    @Nested
    @DisplayName("Set Organisation To Remove")
    @SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.JUnitTestsShouldIncludeAssert", "PMD.ExcessiveImports"})
    class SetOrganisationToRemove {

        private Organisation organisation;
        private OrganisationPolicy organisationPolicy;
        private ChangeOrganisationRequest changeOrganisationRequestBefore;
        private ChangeOrganisationRequest changeOrganisationRequestAfter;
        private CaseDetails callbackCaseDetails;

        @BeforeEach
        void setUp() {
            organisation = Organisation.builder()
                .organisationID("Org1")
                .organisationName("Organisation 1")
                .build();

            organisationPolicy = OrganisationPolicy.builder()
                .organisation(organisation)
                .orgPolicyReference("PolicyRef")
                .orgPolicyCaseAssignedRole("Role1")
                .build();

            changeOrganisationRequestBefore = ChangeOrganisationRequest.builder()
                .organisationToAdd(Organisation.builder().organisationID("123").build())
                .organisationToRemove(Organisation.builder().organisationID(null).build())
                .caseRoleId(DynamicList.builder()
                                .value(DynamicListElement.builder().code("Role1").build())
                                .build())
                .requestTimestamp(LocalDateTime.now())
                .approvalStatus("1")
                .build();

            changeOrganisationRequestAfter = ChangeOrganisationRequest.builder()
                .organisationToAdd(Organisation.builder().organisationID("123").build())
                .organisationToRemove(Organisation.builder().organisationID("Org1").build())
                .caseRoleId(DynamicList.builder()
                                .value(DynamicListElement.builder().code("Role1").build())
                                .build())
                .requestTimestamp(LocalDateTime.now())
                .approvalStatus("1")
                .build();

            given(jacksonUtils.convertValue(any(), eq(JsonNode.class)))
                .willReturn(objectMapper.convertValue(changeOrganisationRequestAfter, JsonNode.class));
        }

        @Test
        @DisplayName("Set Organisation To Remove should return successfully")
        void setOrganisationToRemoveSuccess() {
            callbackCaseDetails = CaseDetails.builder()
                .data(Map.of(
                    "OrganisationPolicyField1",
                    objectMapper.convertValue(organisationPolicy, JsonNode.class),
                    "ChangeOrganisationRequestField",
                    objectMapper.convertValue(changeOrganisationRequestBefore, JsonNode.class)
                ))
                .build();

            given(jacksonUtils.convertValue(any(), eq(OrganisationPolicy.class)))
                .willReturn(organisationPolicy);

            AboutToSubmitCallbackResponse response =
                service.setOrganisationToRemove(callbackCaseDetails,
                                                changeOrganisationRequestBefore,
                                                "ChangeOrganisationRequestField");

            assertThat(response.getData().get("ChangeOrganisationRequestField"))
                .isEqualTo(objectMapper.convertValue(changeOrganisationRequestAfter, JsonNode.class));
        }

        @Test
        @DisplayName("Set Organisation To Remove Should fail when zero organisation policies match case role")
        void setOrganisationToRemoveShouldFailWhenThereAreNoMatchingOrgPolicies() {
            organisationPolicy  = OrganisationPolicy.builder()
                .orgPolicyCaseAssignedRole("role")
                .orgPolicyReference("ref")
                .organisation(organisation)
                .build();

            callbackCaseDetails = CaseDetails.builder()
                .data(Map.of(
                    "OrganisationPolicyField1",
                    objectMapper.convertValue(organisationPolicy, JsonNode.class),
                    "ChangeOrganisationRequestField",
                    objectMapper.convertValue(changeOrganisationRequestBefore, JsonNode.class)
                ))
                .build();

            given(jacksonUtils.convertValue(any(), eq(OrganisationPolicy.class)))
                .willReturn(organisationPolicy);

            NoCException exception = assertThrows(
                NoCException.class,
                () -> service.setOrganisationToRemove(callbackCaseDetails,
                                                      changeOrganisationRequestBefore,
                                                      "ChangeOrganisationRequestField"));

            assertThat(exception.getErrorCode()).isEqualTo(INVALID_CASE_ROLE_FIELD.getErrorCode());

            assertThat(exception.getErrorMessage())
                .isEqualTo(INVALID_CASE_ROLE_FIELD.getErrorMessage());
        }

        @Test
        @DisplayName("Set Organisation To Remove Should fail when more than one organisation policy matches case role")
        void setOrganisationToRemoveShouldFailWhenThereAreMoreThanOneMatchingOrgPolicies() {
            callbackCaseDetails = CaseDetails.builder()
                .data(Map.of(
                    "OrganisationPolicyField1",
                    objectMapper.convertValue(organisationPolicy, JsonNode.class),
                    "OrganisationPolicyField2",
                    objectMapper.convertValue(organisationPolicy, JsonNode.class),
                    "ChangeOrganisationRequestField",
                    objectMapper.convertValue(changeOrganisationRequestBefore, JsonNode.class)
                ))
                .build();

            given(jacksonUtils.convertValue(any(), eq(OrganisationPolicy.class)))
                .willReturn(organisationPolicy);

            NoCException exception = assertThrows(
                NoCException.class,
                () -> service.setOrganisationToRemove(callbackCaseDetails,
                                                      changeOrganisationRequestBefore,
                                                      "ChangeOrganisationRequestField"));

            assertThat(exception.getErrorCode()).isEqualTo(INVALID_CASE_ROLE_FIELD.getErrorCode());

            assertThat(exception.getErrorMessage())
                .isEqualTo(INVALID_CASE_ROLE_FIELD.getErrorMessage());
        }
    }
}
