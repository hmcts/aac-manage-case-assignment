package uk.gov.hmcts.reform.managecase.client.prd;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import feign.Feign;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import uk.gov.hmcts.reform.managecase.config.JacksonObjectMapperConfig;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "referenceData_organisationalExternalUsers", pactVersion = PactSpecVersion.V3)
class PrdApiClientConsumerTest {

    private static final String CONSUMER = "aac_manageCaseAssignment";
    private static final String PROVIDER = "referenceData_organisationalExternalUsers";
    private static final String ORGANISATION_ID = "someOrganisationIdentifier";
    private static final String USER_ID = "41a5dd5f-91c5-4f69-b7e4-3c8a9c4272e9";
    private static final String USER_TOKEN = "Bearer some-access-token";
    private static final String SERVICE_TOKEN = "some-service-token";

    @Pact(consumer = CONSUMER, provider = PROVIDER)
    public RequestResponsePact activeUsersByOrganisation(PactDslWithProvider builder) {
        DslPart response = new PactDslJsonBody()
            .stringType("organisationIdentifier", ORGANISATION_ID)
            .minArrayLike("users", 1)
                .stringType("userIdentifier", USER_ID)
                .stringType("firstName", "Jane")
                .stringType("lastName", "Smith")
                .stringType("email", "jane.smith@example.com")
            .closeObject()
            .closeArray();

        return builder
            .given("Professional users exist for an Active organisation")
            .uponReceiving("a request for active organisation users without roles for case assignment")
            .method("GET")
            .path("/refdata/external/v1/organisations/users")
            .query("status=Active&returnRoles=false")
            .headers(Map.of(
                "Authorization", USER_TOKEN,
                "ServiceAuthorization", SERVICE_TOKEN,
                "Content-Type", "application/json"
            ))
            .willRespondWith()
            .status(200)
            .headers(Map.of("Content-Type", "application/json"))
            .body(response)
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "activeUsersByOrganisation")
    void shouldFindActiveUsersByOrganisation(MockServer mockServer) {
        FindUsersByOrganisationResponse response = client(mockServer).findActiveUsersByOrganisation();

        assertNotNull(response);
        assertEquals(ORGANISATION_ID, response.getOrganisationIdentifier());
        assertNotNull(response.getUsers());
        assertEquals(1, response.getUsers().size());
        ProfessionalUser user = response.getUsers().getFirst();
        assertAll(
            () -> assertEquals(USER_ID, user.getUserIdentifier()),
            () -> assertEquals("Jane", user.getFirstName()),
            () -> assertEquals("Smith", user.getLastName()),
            () -> assertEquals("jane.smith@example.com", user.getEmail())
        );
    }

    private PrdApiClient client(MockServer mockServer) {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getUserBearerToken()).thenReturn(USER_TOKEN);
        when(securityUtils.getS2SToken()).thenReturn(SERVICE_TOKEN);

        PrdApiClientConfig config = new PrdApiClientConfig();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(
            new JacksonObjectMapperConfig().defaultObjectMapper()
        );

        return Feign.builder()
            .contract(new SpringMvcContract())
            .client(config.client())
            .requestInterceptor(config.authHeadersInterceptor(securityUtils))
            .decoder(new SpringDecoder(() -> new HttpMessageConverters(converter)))
            .target(PrdApiClient.class, mockServer.getUrl());
    }
}
