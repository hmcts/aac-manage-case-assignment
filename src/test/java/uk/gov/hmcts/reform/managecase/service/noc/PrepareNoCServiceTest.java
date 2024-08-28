package uk.gov.hmcts.reform.managecase.service.noc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.definitionstore.model.CaseRole;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.DynamicList;
import uk.gov.hmcts.reform.managecase.domain.DynamicListElement;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.repository.DefinitionStoreRepository;
import uk.gov.hmcts.reform.managecase.repository.PrdRepository;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;
import uk.gov.hmcts.reform.managecase.util.JacksonUtils;

import jakarta.validation.ValidationException;
import java.util.ArrayList;
import java.util.Arrays;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CASE_TYPE_ID_EMPTY;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.INVALID_CASE_ROLE_FIELD;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.JURISDICTION_CANNOT_BE_BLANK;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NOC_REQUEST_ONGOING;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_ORGANISATION_POLICY_ON_CASE_DATA;
import static uk.gov.hmcts.reform.managecase.api.errorhandling.ValidationError.NO_SOLICITOR_ORGANISATION_RECORDED_IN_ORG_POLICY;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.CASE_ROLE_ID;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION_TO_ADD;
import static uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails.ORGANISATION_TO_REMOVE;

@SuppressWarnings({"PMD.UseConcurrentHashMap", "PMD.AvoidDuplicateLiterals", "PMD.ExcessiveImports",
    "PMD.TooManyMethods", "PMD.DataflowAnomalyAnalysis"})
class PrepareNoCServiceTest {

    private static final String COR_FIELD_NAME = "ChangeOrganisationRequestField";
    private static final String REFERENCE = "1602246428939903";
    private static final String JURISDICTION = "Jurisdiction";
    private static final String CASE_TYPE = "caseType1";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PrepareNoCService prepareNoCService;

    @Mock
    private PrdRepository prdRepository;

    @Mock
    private DefinitionStoreRepository definitionStoreRepository;

    @Mock
    private SecurityUtils securityUtils;

    private final JacksonUtils jacksonUtils = new JacksonUtils(objectMapper);

    private Map<String, OrganisationPolicy> orgPolicies;

    @BeforeEach
    void setUp() {
        openMocks(this);
        prepareNoCService = new PrepareNoCService(prdRepository,
                                                  securityUtils,
                                                  jacksonUtils,
                                                  definitionStoreRepository);
    }

    @Nested
    @DisplayName("Prepare NoC Request")
    class PrepareNoC {

        @BeforeEach
        void setUp() {
            mockOrgIdentifierInPrdFindUsersByOrganisation("orgName1");
            mockDefinitionStoreCaseRoles();
            orgPolicies = prepareOrganisationPoliciesForSolicitorTest();
        }

        @Test
        @DisplayName("PrepareNoC event successfully returned for a Solicitor user")
        void shouldReturnPrepareNoCEventForSolicitor() {
            mockUserCaseRolesForSolicitor();

            Map<String, JsonNode> caseData = new HashMap<>();
            orgPolicies.forEach((key, value) -> caseData.put(key, organisationPolicyToJsonNode(value)));
            caseData.put(COR_FIELD_NAME, changeOrganisationRequestToJsonNode(changeOrganisationRequest()));

            CaseDetails caseDetails = CaseDetails.builder()
                .id(REFERENCE)
                .jurisdiction(JURISDICTION)
                .caseTypeId(CASE_TYPE)
                .data(caseData)
                .build();

            Map<String, JsonNode> result = prepareNoCService.prepareNoCRequest(caseDetails);

            JsonNode caseRoleId = result.get(COR_FIELD_NAME).findPath(CASE_ROLE_ID);
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
            orgPolicies.forEach((key, value) -> caseData.put(key, organisationPolicyToJsonNode(value)));
            caseData.put(COR_FIELD_NAME, changeOrganisationRequestToJsonNode(changeOrganisationRequest()));

            CaseDetails caseDetails = CaseDetails.builder()
                .id(REFERENCE)
                .jurisdiction(JURISDICTION)
                .caseTypeId(CASE_TYPE)
                .data(caseData)
                .build();

            Map<String, JsonNode> result = prepareNoCService.prepareNoCRequest(caseDetails);

            JsonNode caseRoleId = result.get(COR_FIELD_NAME).findPath(CASE_ROLE_ID);
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

            CaseDetails caseDetails = CaseDetails.builder()
                .id(REFERENCE)
                .jurisdiction(JURISDICTION)
                .caseTypeId(CASE_TYPE)
                .data(caseData)
                .build();

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> prepareNoCService.prepareNoCRequest(caseDetails)
            );
            assertThat(exception.getMessage(), is(NO_ORGANISATION_POLICY_ON_CASE_DATA));
        }

        @Test
        @DisplayName("should error when Solicitor is not recorded on an OrganisationPolicy on the case")
        void shouldErrorWhenSolicitorIsNotRecordedInAnOrganisationPolicyOnTheCase() {
            mockUserCaseRolesForSolicitor();
            mockOrgIdentifierInPrdFindUsersByOrganisation("orgNotInPolicy");

            Map<String, JsonNode> caseData = new HashMap<>();
            orgPolicies.forEach((key, value) -> caseData.put(key, organisationPolicyToJsonNode(value)));
            caseData.put(COR_FIELD_NAME, changeOrganisationRequestToJsonNode(changeOrganisationRequest()));

            CaseDetails caseDetails = CaseDetails.builder()
                .id(REFERENCE)
                .jurisdiction(JURISDICTION)
                .caseTypeId(CASE_TYPE)
                .data(caseData)
                .build();

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> prepareNoCService.prepareNoCRequest(caseDetails)
            );
            assertThat(exception.getMessage(), is(NO_SOLICITOR_ORGANISATION_RECORDED_IN_ORG_POLICY));
        }

        @Test
        @DisplayName("should error when there is ongoing NoCRequest")
        void shouldErrorWhenThereIsOngoingNoCRequest() {
            mockUserCaseRolesForSolicitor();

            Map<String, JsonNode> caseData = new HashMap<>();
            orgPolicies.forEach((key, value) -> caseData.put(key, organisationPolicyToJsonNode(value)));
            caseData.put(COR_FIELD_NAME, changeOrganisationRequestToJsonNode(ongoingChangeOrganisationRequest()));

            CaseDetails caseDetails = CaseDetails.builder()
                .id(REFERENCE)
                .jurisdiction(JURISDICTION)
                .caseTypeId(CASE_TYPE)
                .data(caseData)
                .build();

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> prepareNoCService.prepareNoCRequest(caseDetails)
            );
            assertThat(exception.getMessage(), is(NOC_REQUEST_ONGOING));
        }

        @Test
        @DisplayName("should error when Jurisdiction is missing")
        void shouldErrorWhenMissingJurisdiction() {
            CaseDetails caseDetails = CaseDetails.builder()
                .id(REFERENCE)
                .jurisdiction(null)
                .caseTypeId(CASE_TYPE)
                .build();

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> prepareNoCService.prepareNoCRequest(caseDetails)
            );
            assertThat(exception.getMessage(), is(JURISDICTION_CANNOT_BE_BLANK));
        }

        @Test
        @DisplayName("should error when CaseType is missing")
        void shouldErrorWhenMissingCaseType() {

            CaseDetails caseDetails = CaseDetails.builder()
                .id(REFERENCE)
                .jurisdiction(JURISDICTION)
                .build();

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> prepareNoCService.prepareNoCRequest(caseDetails)
            );
            assertThat(exception.getMessage(), is(CASE_TYPE_ID_EMPTY));
        }

        @Test
        @DisplayName("should error when ChangeOrganisationRequest is missing")
        void shouldErrorWhenMissingChangeOrganisationRequest() {
            mockUserCaseRolesForSolicitor();

            Map<String, JsonNode> caseData = new HashMap<>();
            orgPolicies.forEach((key, value) -> caseData.put(key, organisationPolicyToJsonNode(value)));

            CaseDetails caseDetails = CaseDetails.builder()
                .id(REFERENCE)
                .jurisdiction(JURISDICTION)
                .caseTypeId(CASE_TYPE)
                .data(caseData)
                .build();

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> prepareNoCService.prepareNoCRequest(caseDetails)
            );
            assertThat(exception.getMessage(), is(CHANGE_ORG_REQUEST_FIELD_MISSING_OR_INVALID));
        }

        @Test
        @DisplayName("should error if the ChangeOrganisationRequest.CaseRole is missing ")
        void shouldErrorWhenMissingCaseRoleOnChangeOrganisationRequest() {
            mockUserCaseRolesForSolicitor();

            Map<String, JsonNode> caseData = new HashMap<>();
            orgPolicies.forEach((key, value) -> caseData.put(key, organisationPolicyToJsonNode(value)));
            caseData.put(COR_FIELD_NAME, corWithMissingCaseRoleId());

            CaseDetails caseDetails = CaseDetails.builder()
                .id(REFERENCE)
                .jurisdiction(JURISDICTION)
                .caseTypeId(CASE_TYPE)
                .data(caseData)
                .build();

            ValidationException exception = assertThrows(
                ValidationException.class,
                () -> prepareNoCService.prepareNoCRequest(caseDetails)
            );
            assertThat(exception.getMessage(), is(INVALID_CASE_ROLE_FIELD));
        }

        private ChangeOrganisationRequest changeOrganisationRequest() {
            return ChangeOrganisationRequest.builder()
                .build();
        }

        private ChangeOrganisationRequest ongoingChangeOrganisationRequest() {
            DynamicListElement claimantDLE = DynamicListElement.builder().code("[Claimant]").label("Claimant").build();

            return ChangeOrganisationRequest.builder()
                .organisationToAdd(Organisation.builder().build())
                .organisationToRemove(Organisation.builder().build())
                .caseRoleId(DynamicList.builder()
                                .value(claimantDLE)
                                .listItems(Arrays.asList(claimantDLE))
                                .build())
                .build();
        }

        private JsonNode corWithMissingCaseRoleId() {
            try {
                return objectMapper.readTree(
                    "{\"" + ORGANISATION_TO_ADD + "\":{}, \"" + ORGANISATION_TO_REMOVE + "\":{}}");
            } catch (JsonProcessingException e) {
                fail();
                return null;
            }
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
                return null;
            }
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
            given(securityUtils.hasSolicitorAndJurisdictionRoles(emptyList(), JURISDICTION)).willReturn(true);
        }

        private void mockUserCaseRolesForCaseworker() {
            UserInfo userInfo = new UserInfo("", "", "", "", "", emptyList());
            given(securityUtils.getUserInfo()).willReturn(userInfo);
            given(securityUtils.hasSolicitorAndJurisdictionRoles(emptyList(), JURISDICTION)).willReturn(false);
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
