package uk.gov.hmcts.reform.managecase.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

class PrdRepositoryTest {

    @Mock
    private PrdApiClient apiClient;

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
}
