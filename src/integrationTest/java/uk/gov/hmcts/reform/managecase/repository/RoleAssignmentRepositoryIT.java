package uk.gov.hmcts.reform.managecase.repository;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Test;
import uk.gov.hmcts.reform.managecase.api.errorhandling.BadRequestException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ResourceNotFoundException;
import uk.gov.hmcts.reform.managecase.api.errorhandling.ServiceException;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResource;
import uk.gov.hmcts.reform.managecase.api.payload.RoleAssignmentResponse;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RoleAssignmentRepositoryIT {

    @Inject
    protected RoleAssignmentRepository roleAssignmentRepository;

    private static final String ID = "4d96923f-891a-4cb1-863e-9bec44d1689d";
    private static final String ACTOR_ID_TYPE = "IDAM";
    private static final String ACTOR_ID = "567567";
    private static final String ROLE_TYPE = "ORGANISATION";
    private static final String ROLE_NAME = "judge";
    private static final String CLASSIFICATION = "PUBLIC";
    private static final String GRANT_TYPE = "STANDARD";
    private static final String ROLE_CATEGORY = "JUDICIAL";
    private static final Boolean READ_ONLY = Boolean.FALSE;
    private static final String BEGIN_TIME = "2021-01-01T00:00:00Z";
    private static final String END_TIME = "2223-01-01T00:00:00Z";
    private static final String CREATED = "2020-12-23T06:37:58.000196065Z";
    private static final Instant EXPECTED_BEGIN_TIME = Instant.parse(BEGIN_TIME);
    private static final Instant EXPECTED_END_TIME = Instant.parse(END_TIME);
    private static final Instant EXPECTED_CREATED = Instant.parse(CREATED);
    private static final String ATTRIBUTES_CONTRACT_TYPE = "SALARIED";
    private static final String ATTRIBUTES_JURISDICTION = "divorce";
    private static final String ATTRIBUTES_CASE_ID = "1504259907353529";
    private static final String ATTRIBUTES_REGION = "south-east";
    private static final String ATTRIBUTES_LOCATION = "south-east-cornwall";
    private static final String AUTHORISATIONS_AUTH_1 = "auth1";
    private static final String AUTHORISATIONS_AUTH_2 = "auth2";
    @SuppressWarnings("checkstyle:LineLength") // don't want to break error messages and add unwanted +
    private static final String HTTP_400_ERROR_MESSAGE = "Client error when getting Role Assignments from Role Assignment Service because of ";
    @SuppressWarnings("checkstyle:LineLength") // don't want to break error messages and add unwanted +
    private static final String HTTP_500_ERROR_MESSAGE = "Problem getting Role Assignments from Role Assignment Service because of ";

    private List<String> caseIds = Arrays.asList(new String[]{"111", "222"});
    private List<String> userIds = Arrays.asList(new String[]{"111", "222"});

    @Test
    public void shouldReturnRoleAssignmentsByUserAndRoles() {
        stubFor(WireMock.post(urlMatching("/am/role-assignments/query")).willReturn(okJson(jsonBody(ID))));
        validateRAForFindRoleAssignmentsByCasesAndUsers();
    }

    @Test
    public void shouldErrorOn404WhenPostFindRoleAssignmentsByCasesAndUsers() {
        final String errorMessage = "No Role Assignments found for userIds=" + userIds + " and casesIds=" + userIds;
        stubFor(WireMock.post(urlMatching("/am/role-assignments/query")).willReturn(notFound()));

        final ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds)
        );

        assertThat(exception.getMessage(), startsWith(errorMessage));
    }

    @Test
    public void shouldErrorOn500WhenPostFindRoleAssignmentsByCasesAndUsers() {

        stubFor(WireMock.post(urlMatching("/am/role-assignments/query")).willReturn(serverError()));

        final ServiceException exception = assertThrows(ServiceException.class, () ->
            roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds)
        );

        assertThat(exception.getMessage(), startsWith(HTTP_500_ERROR_MESSAGE));
    }

    @Test
    public void shouldErrorOn400WhenPostFindRoleAssignmentsByCasesAndUsers() {
        stubFor(WireMock.post(urlMatching("/am/role-assignments/query")).willReturn(badRequest()));

        final BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds)
        );
        assertThat(exception.getMessage(), startsWith(HTTP_400_ERROR_MESSAGE));
    }

    private void validateRAForFindRoleAssignmentsByCasesAndUsers() {
        final RoleAssignmentResponse roleAssignments =
            roleAssignmentRepository.findRoleAssignmentsByCasesAndUsers(caseIds, userIds);

        assertThat(roleAssignments.getRoleAssignments().size(), is(1));
        RoleAssignmentResource roleAssignmentResource = roleAssignments.getRoleAssignments().get(0);
        assertThat(roleAssignmentResource.getId(), is(ID));
        assertThat(roleAssignmentResource.getActorIdType(), is(ACTOR_ID_TYPE));
        assertThat(roleAssignmentResource.getActorId(), is(ACTOR_ID));
        assertThat(roleAssignmentResource.getRoleType(), is(ROLE_TYPE));
        assertThat(roleAssignmentResource.getRoleName(), is(ROLE_NAME));
        assertThat(roleAssignmentResource.getClassification(), is(CLASSIFICATION));
        assertThat(roleAssignmentResource.getGrantType(), is(GRANT_TYPE));
        assertThat(roleAssignmentResource.getRoleCategory(), is(ROLE_CATEGORY));
        assertThat(roleAssignmentResource.getReadOnly(), is(READ_ONLY));
        assertThat(roleAssignmentResource.getBeginTime(), is(EXPECTED_BEGIN_TIME));
        assertThat(roleAssignmentResource.getEndTime(), is(EXPECTED_END_TIME));
        assertThat(roleAssignmentResource.getCreated(), is(EXPECTED_CREATED));

        assertThat(roleAssignmentResource.getAttributes().getContractType().get(), is(ATTRIBUTES_CONTRACT_TYPE));
        assertThat(roleAssignmentResource.getAttributes().getJurisdiction().get(), is(ATTRIBUTES_JURISDICTION));
        assertThat(roleAssignmentResource.getAttributes().getCaseId().get(), is(ATTRIBUTES_CASE_ID));
        assertThat(roleAssignmentResource.getAttributes().getLocation().get(), is(ATTRIBUTES_LOCATION));
        assertThat(roleAssignmentResource.getAttributes().getRegion().get(), is(ATTRIBUTES_REGION));

        assertThat(roleAssignmentResource.getAuthorisations().size(), is(2));
        assertThat(roleAssignmentResource.getAuthorisations().get(0), is(AUTHORISATIONS_AUTH_1));
        assertThat(roleAssignmentResource.getAuthorisations().get(1), is(AUTHORISATIONS_AUTH_2));
    }

    private static String jsonBody(String id) {
        return "{\n"
            + "  \"roleAssignmentResponse\": [\n"
            + "    {\n"
            + "      \"id\": \"" + id + "\",\n"
            + "      \"actorIdType\": \"" + ACTOR_ID_TYPE + "\",\n"
            + "      \"actorId\": \"" + ACTOR_ID + "\",\n"
            + "      \"roleType\": \"" + ROLE_TYPE + "\",\n"
            + "      \"roleName\": \"" + ROLE_NAME + "\",\n"
            + "      \"classification\": \"" + CLASSIFICATION + "\",\n"
            + "      \"grantType\": \"" + GRANT_TYPE + "\",\n"
            + "      \"roleCategory\": \"" + ROLE_CATEGORY + "\",\n"
            + "      \"readOnly\": " + READ_ONLY + ",\n"
            + "      \"beginTime\": \"" + BEGIN_TIME + "\",\n"
            + "      \"endTime\": \"" + END_TIME + "\",\n"
            + "      \"created\": \"" + CREATED + "\",\n"
            + "      \"attributes\": {\n"
            + "        \"contractType\": \"" + ATTRIBUTES_CONTRACT_TYPE + "\",\n"
            + "        \"jurisdiction\": \"" + ATTRIBUTES_JURISDICTION + "\",\n"
            + "        \"caseId\": \"" + ATTRIBUTES_CASE_ID + "\",\n"
            + "        \"location\": \"" + ATTRIBUTES_LOCATION + "\",\n"
            + "        \"region\": \"" + ATTRIBUTES_REGION + "\"\n"
            + "      },\n"
            + "      \"authorisations\": [\"" + AUTHORISATIONS_AUTH_1 + "\", \"" + AUTHORISATIONS_AUTH_2 + "\"]\n"
            + "    }\n"
            + "  ]\n"
            + "}";
    }
}
