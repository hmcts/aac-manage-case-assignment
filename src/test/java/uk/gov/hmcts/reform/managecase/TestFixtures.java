package uk.gov.hmcts.reform.managecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.assertj.core.util.Maps;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import uk.gov.hmcts.reform.managecase.client.datastore.CaseDetails;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.domain.Organisation;
import uk.gov.hmcts.reform.managecase.domain.OrganisationPolicy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestFixtures {

    private static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    private static final String CASE_ID = "12345678";
    private static final String JURISDICTION = "AUTOTEST1";
    private static final String ORG_POLICY_ROLE = "caseworker-probate";
    private static final String ORGANIZATION_ID = "TEST_ORG";

    private static final ObjectMapper OBJECT_MAPPER = new Jackson2ObjectMapperBuilder()
            .modules(new Jdk8Module())
            .build();

    private TestFixtures() {
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
                    .data(Maps.newHashMap("OrganisationPolicy1", jsonNode(ORGANIZATION_ID, ORG_POLICY_ROLE)));
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

        public static ProfessionalUser user(String userIdentifier, String... roles) {
            return ProfessionalUser.builder()
                    .userIdentifier(userIdentifier)
                    .roles(List.of(roles))
                    .build();
        }

    }

}
