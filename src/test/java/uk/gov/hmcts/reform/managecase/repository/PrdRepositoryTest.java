package uk.gov.hmcts.reform.managecase.repository;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.managecase.client.prd.FindOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.FindUsersByOrganisationResponse;
import uk.gov.hmcts.reform.managecase.client.prd.PrdApiClient;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

class PrdRepositoryTest {

    private static final String USER_ID = "41a5dd5f-91c5-4f69-b7e4-3c8a9c4272e9";

    @Mock
    private PrdApiClient apiClient;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private DefaultPrdRepository repository;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    void shouldFindRequestingUserOrganisationWithoutLoadingUsers() {
        FindOrganisationResponse response = new FindOrganisationResponse(null, "ORG1234", "Solicitors");
        given(apiClient.findRequestingUserOrganisation()).willReturn(response);

        assertThat(repository.findRequestingUserOrganisation()).isSameAs(response);

        verify(apiClient, never()).findActiveUsersByOrganisation();
        verify(apiClient, never()).findActiveUserOrganisation(anyString());
    }

    @Test
    void shouldUseFilteredUserLookupWhenMetadataIsNotFound() {
        given(apiClient.findRequestingUserOrganisation()).willThrow(prdError(404));
        given(securityUtils.getUserId()).willReturn(USER_ID);
        given(apiClient.findActiveUserOrganisation(USER_ID))
            .willReturn(new FindUsersByOrganisationResponse(null, "ORG1234"));

        assertThat(repository.findRequestingUserOrganisation().getOrganisationIdentifier()).isEqualTo("ORG1234");

        verify(apiClient).findActiveUserOrganisation(USER_ID);
        verify(apiClient, never()).findActiveUsersByOrganisation();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 500, 503})
    void shouldPropagateMetadataFailuresOtherThanNotFound(int status) {
        FeignException failure = prdError(status);
        given(apiClient.findRequestingUserOrganisation()).willThrow(failure);

        assertThat(assertThrows(FeignException.class, repository::findRequestingUserOrganisation)).isSameAs(failure);

        verifyNoInteractions(securityUtils);
        verify(apiClient, never()).findActiveUserOrganisation(anyString());
        verify(apiClient, never()).findActiveUsersByOrganisation();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void shouldRejectMissingUserIdInsteadOfFetchingAllUsers(String userId) {
        given(apiClient.findRequestingUserOrganisation()).willThrow(prdError(404));
        given(securityUtils.getUserId()).willReturn(userId);

        assertThrows(IllegalStateException.class, repository::findRequestingUserOrganisation);

        verify(apiClient, never()).findActiveUserOrganisation(anyString());
        verify(apiClient, never()).findActiveUsersByOrganisation();
    }

    @Test
    void shouldPropagateFallbackLookupFailure() {
        given(apiClient.findRequestingUserOrganisation()).willThrow(prdError(404));
        given(securityUtils.getUserId()).willReturn(USER_ID);
        FeignException failure = prdError(403);
        given(apiClient.findActiveUserOrganisation(USER_ID)).willThrow(failure);

        assertThat(assertThrows(FeignException.class, repository::findRequestingUserOrganisation)).isSameAs(failure);

        verify(apiClient, never()).findActiveUsersByOrganisation();
    }

    @Test
    void shouldCacheOrganisationMetadataSeparatelyForEachUserToken() {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        FindOrganisationResponse firstOrganisation = new FindOrganisationResponse(null, "ORG1234", "First");
        FindOrganisationResponse secondOrganisation = new FindOrganisationResponse(null, "ORG5678", "Second");
        given(apiClient.findRequestingUserOrganisation()).willReturn(firstOrganisation, secondOrganisation);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(CacheConfiguration.class);
            context.registerBean("securityUtils", SecurityUtils.class, () -> securityUtils);
            context.registerBean(PrdApiClient.class, () -> apiClient);
            context.registerBean(CacheManager.class, () -> new ConcurrentMapCacheManager("requestingUserOrganisation"));
            context.registerBean(DefaultPrdRepository.class);
            context.refresh();
            PrdRepository cachedRepository = context.getBean(PrdRepository.class);

            given(securityUtils.getUserToken()).willReturn("first-user-token");
            assertThat(cachedRepository.findRequestingUserOrganisation()).isSameAs(firstOrganisation);
            assertThat(cachedRepository.findRequestingUserOrganisation()).isSameAs(firstOrganisation);

            given(securityUtils.getUserToken()).willReturn("second-user-token");
            assertThat(cachedRepository.findRequestingUserOrganisation()).isSameAs(secondOrganisation);
            assertThat(cachedRepository.findRequestingUserOrganisation()).isSameAs(secondOrganisation);
        }

        verify(apiClient, times(2)).findRequestingUserOrganisation();
        verify(apiClient, never()).findActiveUsersByOrganisation();
    }

    @Test
    @DisplayName("Find users in the invokers organisation")
    void shouldFindUsersByOrganisation() {

        FindUsersByOrganisationResponse response = new FindUsersByOrganisationResponse(null, null);

        given(apiClient.findActiveUsersByOrganisation()).willReturn(response);

        FindUsersByOrganisationResponse result = repository.findUsersByOrganisation();

        assertThat(result).isSameAs(response);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableCaching
    static class CacheConfiguration {
    }

    private FeignException prdError(int status) {
        Request request = Request.create(Request.HttpMethod.GET,
            "http://prd/refdata/external/v1/organisations/metadata", Map.of(), null, new RequestTemplate());
        return FeignException.errorStatus("findRequestingUserOrganisation",
            Response.builder().status(status).request(request).build());
    }
}
