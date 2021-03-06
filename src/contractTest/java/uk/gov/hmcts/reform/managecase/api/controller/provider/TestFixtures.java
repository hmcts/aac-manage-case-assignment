package uk.gov.hmcts.reform.managecase.api.controller.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.util.Maps;
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

    public static final String CASE_TYPE_ID = "TEST_CASE_TYPE";
    public static final String CASE_ID = "1583841721773828";
    public static final String JURISDICTION = "AUTOTEST1";
    public static final String CASE_ROLE = "[Collaborator]";
    public static final String ORGANIZATION_ID = "TEST_ORG";

    public static final String FIRST_NAME = "Bill";
    public static final String LAST_NAME = "Roberts";
    public static final String EMAIL = "bill.roberts@greatbrsolicitors.co.uk";
    public static final String IDAM_STATUS = "ACTIVE";


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
                .id(CASE_ID)
                .jurisdiction(JURISDICTION)
                .state(null)
                .data(Maps.newHashMap("OrganisationPolicy1", jsonNode(ORGANIZATION_ID, CASE_ROLE)));
        }

        public static OrganisationPolicy organisationPolicy(String organizationId, String orgPolicyRole) {
            return OrganisationPolicy.builder()
                .orgPolicyCaseAssignedRole(orgPolicyRole)
                .orgPolicyReference(null)
                .organisation(new Organisation(organizationId, organizationId))
                .build();
        }

        private static JsonNode jsonNode(String organizationId, String orgPolicyRole) {
            return new ObjectMapper().convertValue(organisationPolicy(organizationId, orgPolicyRole), JsonNode.class);
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
                .idamStatus(IDAM_STATUS)
                .build();
        }

    }

}
