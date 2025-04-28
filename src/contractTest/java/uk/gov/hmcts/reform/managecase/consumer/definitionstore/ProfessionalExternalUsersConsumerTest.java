package uk.gov.hmcts.reform.managecase.consumer.definitionstore;

import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactFolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.PrdApiClient;
import uk.gov.hmcts.reform.managecase.client.prd.ProfessionalUser;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "referenceData_organisationalExternalUsers", hostInterface = "localhost", port = "5544")
@PactFolder("pacts")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ProfessionalExternalUsersConsumerTest {

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean(name = "securityUtils")
    private SecurityUtils securityUtils;

    @Inject
    private PrdApiClient refdataClient;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String AUTHORIZATION_TOKEN = "Bearer some-access-token";
    private static final String SERVICE_AUTHORIZATION_HEADER = "ServiceAuthorization";
    private static final String SERVICE_AUTH_TOKEN = "someServiceAuthToken";


    @BeforeEach
    void setUpTokens() {
        when(securityUtils.getS2SToken()).thenReturn(SERVICE_AUTH_TOKEN);
        when(securityUtils.getUserBearerToken()).thenReturn(AUTHORIZATION_TOKEN);
    }

    @Pact(consumer = "aac_manageCaseAssignment", provider = "referenceData_organisationalExternalUsers")
    public RequestResponsePact generatePactFragmentForGetUserOrganisation(PactDslWithProvider builder) {
        return builder
            .given("Organisation with Id exists")
            .uponReceiving("A Request to get organisation for user")
            .method("GET")
            .headers(
                SERVICE_AUTHORIZATION_HEADER, SERVICE_AUTH_TOKEN,
                AUTHORIZATION_HEADER, AUTHORIZATION_TOKEN
            )
            .path("/refdata/external/v1/organisations/users")
            .query("status=Active&returnRoles=false")
            .willRespondWith()
            .body(buildOrganisationResponseBody())
            .status(HttpStatus.OK.value())
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "generatePactFragmentForGetUserOrganisation")
    public void verifyUserOrganisation() {
        FindUsersByOrganisationResponse response = refdataClient.findActiveUsersByOrganisation();
        assertThat(response).isNotNull();
        assertThat(response.getOrganisationIdentifier()).isEqualTo("someOrganisationIdentifier");
        assertThat(response.getUsers().size()).isGreaterThan(0);
        ProfessionalUser user = response.getUsers().getFirst();
        assertThat(user.getUserIdentifier()).isEqualTo("userId");
        assertThat(user.getFirstName()).isEqualTo("userFirstName");
        assertThat(user.getLastName()).isEqualTo("userLastName");
        assertThat(user.getEmail()).isEqualTo("email@example.org");
        assertThat(user.getIdamStatus()).isEqualTo("ACTIVE");
    }

    private PactDslJsonBody buildOrganisationResponseBody() {
        return new PactDslJsonBody()
            .stringType("organisationIdentifier", "someOrganisationIdentifier")
            .minArrayLike("users", 1, 1)
                .stringType("userIdentifier", "userId")
                .stringType("firstName", "userFirstName")
                .stringType("lastName", "userLastName")
                .stringType("email", "email@example.org")
                .stringType("idamStatus", "ACTIVE")
            .closeArray()
            .asBody();
    }
}
