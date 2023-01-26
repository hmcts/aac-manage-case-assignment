package uk.gov.hmcts.reform.managecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.assertj.core.util.Maps;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentAttributesResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.datastore.model.CaseViewField;
import uk.gov.hmcts.reform.managecase.client.datastore.model.FieldTypeDefinition;
import uk.gov.hmcts.reform.managecase.client.datastore.model.WizardPage;
import uk.gov.hmcts.reform.managecase.client.datastore.model.WizardPageComplexFieldOverride;
import uk.gov.hmcts.reform.managecase.client.datastore.model.WizardPageField;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignedUsers;
import uk.gov.hmcts.reform.managecase.domain.ChangeOrganisationRequest;
import uk.gov.hmcts.reform.managecase.domain.GrantType;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.domain.UserDetails;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.managecase.repository.DefaultDataStoreRepository.CHANGE_ORGANISATION_REQUEST;

public class TestFixtures {

    public static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    public static final String CASE_ID = "1588234985453946";
    public static final String JURISDICTION = "AUTOTEST1";
    public static final String CASE_ROLE = "[Collaborator]";
    public static final String CASE_ROLE2 = "[Creator]";
    public static final String ORGANIZATION_ID = "TEST_ORG";

    public static final String FIRST_NAME = "Bill";
    public static final String LAST_NAME = "Roberts";
    public static final String EMAIL = "bill.roberts@greatbrsolicitors.co.uk";
    public static final String IDAM_STATUS = "ACTIVE";

    private static final ObjectMapper OBJECT_MAPPER = new Jackson2ObjectMapperBuilder()
            .modules(new Jdk8Module())
            .build();
    public static final String IDAM_ID = "221a2877-e1ab-4dc4-a9ff-f9424ad58738";

    private TestFixtures() {
    }

    public static class IdamFixture {

        private IdamFixture() {
        }

        @SuppressWarnings("PMD.LawOfDemeter")
        public static uk.gov.hmcts.reform.idam.client.models.UserDetails userDetails(String id, String... roles) {
            return uk.gov.hmcts.reform.idam.client.models.UserDetails.builder()
                    .id(id)
                    .forename(FIRST_NAME)
                    .surname(LAST_NAME)
                    .roles(List.of(roles))
                    .build();
        }
    }

    public static class CaseDetailsFixture {

        private CaseDetailsFixture() {
        }

        public static CaseDetails caseDetails() {
            return defaultCaseDetails().build();
        }

        public static CaseDetails caseDetails(ChangeOrganisationRequest changeOrganisationRequest) {
            return defaultCaseDetails()
                .data(Map.of("changeOrganisationRequestField",
                             OBJECT_MAPPER.convertValue(changeOrganisationRequest, JsonNode.class)))
                .build();
        }

        public static CaseDetails caseDetails(ChangeOrganisationRequest changeOrganisationRequest,
                                              OrganisationPolicy organisationPolicy) {
            return defaultCaseDetails()
                .data(Map.of("changeOrganisationRequestField",
                             OBJECT_MAPPER.convertValue(changeOrganisationRequest, JsonNode.class),
                             "organisationPolicyField",
                             OBJECT_MAPPER.convertValue(organisationPolicy, JsonNode.class)))
                .build();
        }

        public static CaseDetails caseDetails(String organizationId, String... orgPolicyRoles) {
            Map<String, JsonNode> jsonNodeMap = Stream.of(orgPolicyRoles)
                    .collect(Collectors.toMap(role -> "Field_" + role,
                        role -> organisationPolicyJsonNode(organizationId, role),
                        (v1, v2) -> v1, LinkedHashMap::new));
            return defaultCaseDetails().data(jsonNodeMap).build();
        }

        public static CaseDetails.CaseDetailsBuilder defaultCaseDetails() {
            return CaseDetails.builder()
                .caseTypeId(CASE_TYPE_ID)
                .id(CASE_ID)
                .jurisdiction(JURISDICTION)
                .state(null)
                .data(Maps.newHashMap("OrganisationPolicy1", organisationPolicyJsonNode(ORGANIZATION_ID, CASE_ROLE)));
        }

        public static OrganisationPolicy organisationPolicy(String organizationId, String orgPolicyRole) {
            return OrganisationPolicy.builder()
                    .orgPolicyCaseAssignedRole(orgPolicyRole)
                    .orgPolicyReference(null)
                    .organisation(Organisation.builder().organisationID(organizationId).build())
                    .build();
        }

        public static JsonNode organisationPolicyJsonNode(String organizationId, String orgPolicyRole) {
            return OBJECT_MAPPER.convertValue(organisationPolicy(organizationId, orgPolicyRole), JsonNode.class);
        }
    }

    public static class ProfessionalUserFixture {

        private ProfessionalUserFixture() {
        }

        public static FindUsersByOrganisationResponse usersByOrganisation(ProfessionalUser... users) {
            return usersByOrganisation(ORGANIZATION_ID, List.of(users));
        }

        public static FindUsersByOrganisationResponse usersByOrganisation(String organisationId,
                                                                          List<ProfessionalUser> users) {
            return new FindUsersByOrganisationResponse(users, organisationId);
        }

        public static ProfessionalUser user(String userIdentifier) {
            return ProfessionalUser.builder()
                    .userIdentifier(userIdentifier)
                    .firstName(FIRST_NAME)
                    .lastName(LAST_NAME)
                    .email(EMAIL)
                    .idamStatus(IDAM_STATUS)
                    .build();
        }
    }

    public static class RoleAssignmentsFixture {

        private RoleAssignmentsFixture() {
        }

        public static RoleAssignmentResponse roleAssignmentResponse(RoleAssignmentResource... roleAssignments) {
            return RoleAssignmentResponse.builder()
                .roleAssignments(List.of(roleAssignments))
                .build();
        }

        public static RoleAssignmentResource roleAssignment(String caseId, String userId, String roleName) {
            return RoleAssignmentResource.builder()
                .actorIdType("IDAM")
                .actorId(userId)
                .roleType("CASE")
                .roleName(roleName)
                .classification("PUBLIC")
                .grantType(GrantType.SPECIFIC.name())
                .roleCategory("PROFESSIONAL")
                .readOnly(true)
                .authorisations(Collections.emptyList())
                .attributes(RoleAssignmentAttributesResource.builder()
                                .caseId(Optional.of(caseId))
                                .build())
                .build();
        }

    }

    public static class CaseAssignedUsersFixture {

        private CaseAssignedUsersFixture() {
        }

        public static CaseAssignedUsers caseAssignedUsers() {
            return CaseAssignedUsers.builder()
                    .caseId(CASE_ID)
                    .users(List.of(defaultUser()))
                    .build();
        }

        public static UserDetails defaultUser() {
            return UserDetails.builder()
                    .idamId(IDAM_ID)
                    .firstName(FIRST_NAME)
                    .lastName(LAST_NAME)
                    .email(EMAIL)
                    .caseRoles(List.of(CASE_ROLE, CASE_ROLE2))
                    .build();
        }
    }

    public static class CaseUpdateViewEventFixture {

        public static final String CHANGE_ORGANISATION_REQUEST_FIELD = "changeOrganisationRequestField";

        public static List<CaseViewField> getCaseViewFields() {
            FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
            fieldTypeDefinition.setId(CHANGE_ORGANISATION_REQUEST);

            CaseViewField caseViewFields = new CaseViewField();
            caseViewFields.setId(CHANGE_ORGANISATION_REQUEST_FIELD);
            caseViewFields.setFieldTypeDefinition(fieldTypeDefinition);
            return List.of(caseViewFields);
        }

        public static List<WizardPage> getWizardPages() {
            return getWizardPages(CHANGE_ORGANISATION_REQUEST_FIELD);
        }

        public static List<WizardPage> getWizardPages(String caseFieldId) {
            WizardPageField wizardPageField = new WizardPageField();

            WizardPageComplexFieldOverride wizardPageComplexFieldOverride = new WizardPageComplexFieldOverride();
            wizardPageComplexFieldOverride.setComplexFieldElementId(caseFieldId + ".ApprovalStatus");
            wizardPageComplexFieldOverride.setDefaultValue(caseFieldId);
            wizardPageField.setCaseFieldId(caseFieldId);
            wizardPageField.setComplexFieldOverrides(List.of(wizardPageComplexFieldOverride));

            WizardPage wizardPage =  new WizardPage();
            wizardPage.setWizardPageFields(List.of(wizardPageField));
            return List.of(wizardPage);
        }
    }
}
