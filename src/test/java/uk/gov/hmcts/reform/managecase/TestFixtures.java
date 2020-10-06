package uk.gov.hmcts.reform.managecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.assertj.core.util.Maps;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.CaseAssignedUsers;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;
import uk.gov.hmcts.reform.managecase.domain.UserDetails;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private static final ObjectMapper OBJECT_MAPPER = new Jackson2ObjectMapperBuilder()
            .modules(new Jdk8Module())
            .build();
    public static final String IDAM_ID = "221a2877-e1ab-4dc4-a9ff-f9424ad58738";

    private TestFixtures() {
    }

    public static class IdamFixture {

        private IdamFixture() {
        }

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

        public static CaseDetails caseDetails(String organizationId, String... orgPolicyRoles) {
            Map<String, JsonNode> jsonNodeMap = Stream.of(orgPolicyRoles)
                    .collect(Collectors.toMap(role -> "Field_" + role, role -> jsonNode(organizationId, role),
                        (v1, v2) -> v1, LinkedHashMap::new));
            return defaultCaseDetails().data(jsonNodeMap).build();
        }

        public static CaseDetails caseDetails() {
            return defaultCaseDetails().build();
        }

        public static CaseDetails.CaseDetailsBuilder defaultCaseDetails() {
            return CaseDetails.builder()
                    .caseTypeId(CASE_TYPE_ID)
                    .reference(CASE_ID)
                    .jurisdiction(JURISDICTION)
                    .data(Maps.newHashMap("OrganisationPolicy1", jsonNode(ORGANIZATION_ID, CASE_ROLE)));
        }

        public static OrganisationPolicy organisationPolicy(String organizationId, String orgPolicyRole) {
            return OrganisationPolicy.builder()
                    .orgPolicyCaseAssignedRole(orgPolicyRole)
                    .organisation(new Organisation(organizationId, organizationId))
                    .build();
        }

        private static JsonNode jsonNode(String organizationId, String orgPolicyRole) {
            return OBJECT_MAPPER.convertValue(organisationPolicy(organizationId, orgPolicyRole), JsonNode.class);
        }
    }

    public static class ProfessionalUserFixture {

        private ProfessionalUserFixture() {
        }

        public static FindUsersByOrganisationResponse usersByOrganisation(ProfessionalUser... users) {
            return new FindUsersByOrganisationResponse(List.of(users), ORGANIZATION_ID);
        }

        public static ProfessionalUser user(String userIdentifier) {
            return ProfessionalUser.builder()
                    .userIdentifier(userIdentifier)
                    .firstName(FIRST_NAME)
                    .lastName(LAST_NAME)
                    .email(EMAIL)
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

}
