package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.model.SecurityClassification;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.managecase.service.noc.PrepareNoCService.COR_CASE_ROLE_ID;

@SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.AvoidDuplicateLiterals", "PMD.ExcessiveImports",
    "PMD.TooManyMethods", "PMD.DataflowAnomalyAnalysis"})
class PrepareNoCServiceTest {

    private static final String COR_FIELD_NAME = "ChangeOrganisationRequestField";
    private static final String REFERENCE = "1602246428939903";
    private static final String JURISDICTION = "Jurisdiction";
    private static final String CASE_TYPE = "caseType1";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PrepareNoCService prepareNoCService;

    @Mock
    private PrdRepository prdRepository;

    @Mock
    private DefinitionStoreRepository definitionStoreRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private JacksonUtils jacksonUtils;

    private Map<String, OrganisationPolicy> orgPolicies;
    private final SecurityClassification securityClassification = SecurityClassification.PUBLIC;
    private final Map<String, JsonNode> dataClassification = new HashMap<>();

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Nested
    @DisplayName("Prepare NoC Request")
    class PrepareNoC {

        @BeforeEach
        void setUp() {
            mockOrgIdentifierInPrdFindUsersByOrganisation("orgName1");
            mockDefinitionStoreCaseRoles();
            orgPolicies = prepareOrganisationPoliciesForSolicitorTest();
            mockOrganisationPolicies(orgPolicies);
        }

        @Test
        @DisplayName("PrepareNoC event successfully returned for a Solicitor user")
        void shouldReturnPrepareNoCEventForSolicitor() {
            mockUserCaseRolesForSolicitor();

            Map<String, JsonNode> caseData = new HashMap<>();
            orgPolicies.forEach((key, value) -> caseData.put(key, organisationPolicyToJsonNode(value)));
            caseData.put(COR_FIELD_NAME, changeOrganisationRequestToJsonNode(changeOrganisationRequest()));
            CaseDetails caseDetails = new CaseDetails(REFERENCE, JURISDICTION, "", CASE_TYPE, caseData,
                                                      securityClassification, dataClassification);

            Map<String, JsonNode> result = prepareNoCService.prepareNoCRequest(caseDetails);

            JsonNode caseRoleId = result.get(COR_FIELD_NAME).findPath(COR_CASE_ROLE_ID);
            assertEquals("[Claimant]", caseRoleId.findPath("value").findPath("code").textValue());
            assertEquals("Claimant", caseRoleId.findPath("value").findPath("label").textValue());
            assertTrue(caseRoleId.findPath("list_items").isArray());
            assertEquals(2, caseRoleId.findPath("list_items").size());
            assertEquals("[Claimant]", caseRoleId.findPath("list_items").get(0).findPath("code").textValue());
            assertEquals("Claimant", caseRoleId.findPath("list_items").get(0).findPath("label").textValue());
            assertEquals("[Defendant]", caseRoleId.findPath("list_items").get(1).findPath("code").textValue());
            assertEquals("Defendant", caseRoleId.findPath("list_items").get(1).findPath("label").textValue());
        }

        @Test
        @DisplayName("PrepareNoC event successfully returned for a caseworker user")
        void shouldReturnPrepareNoCEventForCaseworker() {
            mockUserCaseRolesForCaseworker();

            Map<String, JsonNode> caseData = new HashMap<>();
            orgPolicies = prepareOrganisationPoliciesForCaseworkerTest();
            mockOrganisationPolicies(orgPolicies);
            orgPolicies.forEach((key, value) -> caseData.put(key, organisationPolicyToJsonNode(value)));
            caseData.put(COR_FIELD_NAME, changeOrganisationRequestToJsonNode(changeOrganisationRequest()));

            CaseDetails caseDetails = new CaseDetails(REFERENCE, JURISDICTION, "", CASE_TYPE, caseData,
                                                      securityClassification, dataClassification);

            Map<String, JsonNode> result = prepareNoCService.prepareNoCRequest(caseDetails);

            JsonNode caseRoleId = result.get(COR_FIELD_NAME).findPath(COR_CASE_ROLE_ID);
            assertEquals("[Claimant]", caseRoleId.findPath("value").findPath("code").textValue());
            assertEquals("Claimant", caseRoleId.findPath("value").findPath("label").textValue());
            assertTrue(caseRoleId.findPath("list_items").isArray());
            assertEquals(2, caseRoleId.findPath("list_items").size());
            assertEquals("[Claimant]", caseRoleId.findPath("list_items").get(0).findPath("code").textValue());
            assertEquals("Claimant", caseRoleId.findPath("list_items").get(0).findPath("label").textValue());
            assertEquals("[Defendant]", caseRoleId.findPath("list_items").get(1).findPath("code").textValue());
            assertEquals("Defendant", caseRoleId.findPath("list_items").get(1).findPath("label").textValue());
        }

        @Test
        @DisplayName("should error when there is no Organisation policy records on the case")
        void shouldErrorWhenThereIsNoOrganisationPolicyRecordsOnTheCase() {
            mockUserCaseRolesForSolicitor();

            Map<String, JsonNode> caseData = new HashMap<>();
            caseData.put(COR_FIELD_NAME, changeOrganisationRequestToJsonNode(changeOrganisationRequest()));
            CaseDetails caseDetails = new CaseDetails(REFERENCE, JURISDICTION, "", CASE_TYPE, caseData,
                                                      securityClassification, dataClassification);

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> prepareNoCService.prepareNoCRequest(caseDetails)
            );
            assertThat(
                exception.getMessage(),
                is("No OrganisationPolicy found on the case data.")
            );
        }

        @Test
        @DisplayName("should error when Solicitor is not recorded on an OrganisationPolicy on the case")
        void shouldErrorWhenSolicitorIsNotRecordedInAnOrganisationPolicyOnTheCase() {
            mockUserCaseRolesForSolicitor();
            mockOrgIdentifierInPrdFindUsersByOrganisation("orgNotInPolicy");

            Map<String, JsonNode> caseData = new HashMap<>();
            orgPolicies.forEach((key, value) -> caseData.put(key, organisationPolicyToJsonNode(value)));
            caseData.put(COR_FIELD_NAME, changeOrganisationRequestToJsonNode(changeOrganisationRequest()));
            CaseDetails caseDetails = new CaseDetails(REFERENCE, JURISDICTION, "", CASE_TYPE, caseData,
                                                      securityClassification, dataClassification);

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> prepareNoCService.prepareNoCRequest(caseDetails)
            );
            assertThat(
                exception.getMessage(),
                is("The Organisation of the solicitor is not recorded in an Org policy on the case.")
            );
        }

        @Test
        @DisplayName("should error when there is ongoing NoCRequest")
        void shouldErrorWhenThereIsOngoingNoCRequest() {
            mockUserCaseRolesForSolicitor();

            Map<String, JsonNode> caseData = new HashMap<>();
            orgPolicies.forEach((key, value) -> caseData.put(key, organisationPolicyToJsonNode(value)));
            caseData.put(COR_FIELD_NAME, changeOrganisationRequestToJsonNode(ongoingChangeOrganisationRequest()));

            CaseDetails caseDetails = new CaseDetails(REFERENCE, JURISDICTION, "", CASE_TYPE, caseData,
                                                      securityClassification, dataClassification);

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> prepareNoCService.prepareNoCRequest(caseDetails)
            );
            assertThat(
                exception.getMessage(),
                is("There is an ongoing NoCRequest.")
            );
        }

        @Test
        @DisplayName("should error when Jurisdiction is missing")
        void shouldErrorWhenMissingJurisdiction() {
            CaseDetails caseDetails = new CaseDetails(REFERENCE, null, "", CASE_TYPE, new HashMap<>(),
                                                      securityClassification, dataClassification);

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> prepareNoCService.prepareNoCRequest(caseDetails)
            );
            assertThat(
                exception.getMessage(),
                is("Jurisdiciton cannot be blank.")
            );
        }

        @Test
        @DisplayName("should error when CaseType is missing")
        void shouldErrorWhenMissingCaseType() {
            CaseDetails caseDetails = new CaseDetails(REFERENCE, JURISDICTION, "", null, new HashMap<>(),
                                                      securityClassification, dataClassification);

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> prepareNoCService.prepareNoCRequest(caseDetails)
            );
            assertThat(
                exception.getMessage(),
                is("CaseType cannot be blank.")
            );
        }

        @Test
        @DisplayName("should error when ChangeOrganisationRequest is missing")
        void shouldErrorWhenMissingChangeOrganisationRequest() {
            mockUserCaseRolesForSolicitor();

            Map<String, JsonNode> caseData = new HashMap<>();
            orgPolicies.forEach((key, value) -> caseData.put(key, organisationPolicyToJsonNode(value)));
            CaseDetails caseDetails = new CaseDetails(REFERENCE, JURISDICTION, "", CASE_TYPE, caseData,
                                                      securityClassification, dataClassification);

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> prepareNoCService.prepareNoCRequest(caseDetails)
            );
            assertThat(
                exception.getMessage(),
                is("Missing ChangeOrganisationRequest field on the case data.")
            );
        }

        private ChangeOrganisationRequest changeOrganisationRequest() {
            return ChangeOrganisationRequest.builder()
                .build();
        }

        private ChangeOrganisationRequest ongoingChangeOrganisationRequest() {
            return ChangeOrganisationRequest.builder()
                .organisationToAdd(Organisation.builder().build())
                .organisationToRemove(Organisation.builder().build())
                .caseRoleId("[Claimant]")
                .build();
        }

        private JsonNode missingCaseRoleId() {
            return objectToJsonNode("{}");
        }

        private JsonNode changeOrganisationRequestToJsonNode(ChangeOrganisationRequest value) {
            return objectToJsonNode(value);
        }

        private JsonNode organisationPolicyToJsonNode(OrganisationPolicy value) {
            return objectToJsonNode(value);
        }

        private JsonNode objectToJsonNode(Object obj) {
            try {
                return objectMapper.readTree(objectMapper.writeValueAsString(obj));
            } catch (JsonProcessingException e) {
                fail();
            }
            return null;
        }

        private Map<String, OrganisationPolicy> prepareOrganisationPoliciesForSolicitorTest() {
            Map<String, OrganisationPolicy> orgPolicies = new HashMap<>();
            orgPolicies.put(
                "orgPolicy1",
                createOrganisationPolicy("[Claimant]", "ref1", createOrganisation("orgName1"))
            );
            orgPolicies.put(
                "orgPolicy2",
                createOrganisationPolicy("[Defendant]", "ref2", createOrganisation("orgName1"))
            );
            orgPolicies.put(
                "orgPolicy3",
                createOrganisationPolicy("[Claimant1]", "ref3", createOrganisation(null))
            );

            return orgPolicies;
        }

        private Map<String, OrganisationPolicy> prepareOrganisationPoliciesForCaseworkerTest() {
            Map<String, OrganisationPolicy> orgPolicies = new HashMap<>();
            orgPolicies.put(
                "orgPolicy1",
                createOrganisationPolicy("[Claimant]", "ref1", createOrganisation("orgName1"))
            );
            orgPolicies.put(
                "orgPolicy2",
                createOrganisationPolicy("[Defendant]", "ref2", createOrganisation("orgName2"))
            );
            orgPolicies.put("orgPolicy3", createOrganisationPolicy("[Claimant1]", "ref3", createOrganisation(null)));

            return orgPolicies;
        }

        private void mockOrganisationPolicies(Map<String, OrganisationPolicy> orgPolicies) {
            given(jacksonUtils.convertValue(any(ObjectNode.class), eq(OrganisationPolicy.class)))
                .willReturn(
                    orgPolicies.get("orgPolicy1"),
                    orgPolicies.get("orgPolicy2"),
                    orgPolicies.get("orgPolicy3")
                );
        }

        private void mockDefinitionStoreCaseRoles() {
            List<CaseRole> caseRoleList = new ArrayList<>();
            caseRoleList.add(CaseRole.builder().id("[CLAIMANT]").name("Claimant").build());
            caseRoleList.add(CaseRole.builder().id("[DEFENDANT]").name("Defendant").build());
            caseRoleList.add(CaseRole.builder().id("[CLAIMANT1]").name("Claimant1").build());
            caseRoleList.add(CaseRole.builder().id("[OTHER]").name("Other role").build());
            given(definitionStoreRepository.caseRoles("0", JURISDICTION, CASE_TYPE)).willReturn(caseRoleList);
        }

        private void mockOrgIdentifierInPrdFindUsersByOrganisation(String orgIdentifier) {
            FindUsersByOrganisationResponse usersByOrganisation = new FindUsersByOrganisationResponse(
                emptyList(),
                orgIdentifier
            );
            given(prdRepository.findUsersByOrganisation()).willReturn(usersByOrganisation);
        }

        private void mockUserCaseRolesForSolicitor() {
            UserInfo userInfo = new UserInfo("", "", "", "", "", emptyList());
            given(securityUtils.getUserInfo()).willReturn(userInfo);
            given(securityUtils.hasSolicitorRole(emptyList(), JURISDICTION)).willReturn(true);
        }

        private void mockUserCaseRolesForCaseworker() {
            UserInfo userInfo = new UserInfo("", "", "", "", "", emptyList());
            given(securityUtils.getUserInfo()).willReturn(userInfo);
            given(securityUtils.hasSolicitorRole(emptyList(), JURISDICTION)).willReturn(false);
        }

        private OrganisationPolicy createOrganisationPolicy(String caseAssignedRole,
                                                            String reference,
                                                            Organisation organisation) {
            return OrganisationPolicy.builder()
                .organisation(organisation)
                .orgPolicyCaseAssignedRole(caseAssignedRole)
                .orgPolicyReference(reference)
                .build();
        }

        private Organisation createOrganisation(String organisationId) {
            return Organisation.builder().organisationID(organisationId).build();
        }
    }

}
