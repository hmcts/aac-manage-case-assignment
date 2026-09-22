package uk.gov.hmcts.reform.managecase.gatewayfilters;

import com.auth0.jwt.exceptions.JWTDecodeException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

class GatewayFiltersTest {

    private static final String DATA_STORE_PATH = "/ccd/searchCases?ctid=CT_MasterCase";
    private static final String DEFINITION_STORE_PATH = "/ccd/case-types/CT_MasterCase";
    private static final String SERVICE_NAME = "xui_webapp";
    private static final String DEFINITION_STORE_SERVICE_NAME = "xui_manage_org";

    @Test
    void allowsConfiguredDataStoreUrlIncludingQueryString() throws Exception {
        ApplicationParams applicationParams = applicationParams(List.of("/searchCases.*"), List.of());
        ServerRequest request = request(DATA_STORE_PATH, applicationParams);

        ServerResponse response = AllowedRoutesFilter.allowedRoutesFilter()
            .filter(request, nextHandler());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void rejectsUrlNotConfiguredForDataStoreOrDefinitionStore() {
        ApplicationParams applicationParams = applicationParams(List.of("/searchCases.*"), List.of());
        ServerRequest request = request("/ccd/not-allowed", applicationParams);
        HandlerFunction<ServerResponse> nextHandler = nextHandler();
        var filter = AllowedRoutesFilter.allowedRoutesFilter();

        assertThatThrownBy(() -> filter.filter(request, nextHandler))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Uri not allowed: /ccd/not-allowed");
    }

    @Test
    void filterSuppliersExposeFilterMethods() {
        assertThat(new AllowedRoutesFilter.FilterSupplier().get()).hasSize(1);
        assertThat(new ValidateClientFilter.FilterSupplier().get()).hasSize(1);
    }

    @Test
    void forwardsRequestWithReplacementAuthHeadersForAllowedService() throws Exception {
        ApplicationParams applicationParams = applicationParams(List.of("/searchCases.*"), List.of());
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getServiceNameFromS2SToken("incoming-s2s-token")).thenReturn(SERVICE_NAME);
        when(securityUtils.getCaaSystemUserToken()).thenReturn("caa-token");
        when(securityUtils.getS2SToken()).thenReturn("outgoing-s2s-token");
        ServerRequest request = request(DATA_STORE_PATH, applicationParams, securityUtils, "incoming-s2s-token");

        AtomicReference<ServerRequest> forwardedRequest = new AtomicReference<>();
        ValidateClientFilter.validateClientFilter().filter(request, forwarded -> {
            forwardedRequest.set(forwarded);
            return ServerResponse.ok().build();
        });

        HttpHeaders headers = forwardedRequest.get().headers().asHttpHeaders();
        assertThat(headers.getFirst(ValidateClientFilter.X_FORWARDED_PREFIX)).isEqualTo("/ccd");
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("caa-token");
        assertThat(headers.getFirst(SERVICE_AUTHORIZATION)).isEqualTo("outgoing-s2s-token");
    }

    @Test
    void rejectsRequestForUnknownService() {
        ApplicationParams applicationParams = applicationParams(List.of("/searchCases.*"), List.of());
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getServiceNameFromS2SToken("incoming-s2s-token")).thenReturn("unknown-service");
        ServerRequest request = request(DATA_STORE_PATH, applicationParams, securityUtils, "incoming-s2s-token");
        HandlerFunction<ServerResponse> nextHandler = nextHandler();
        var filter = ValidateClientFilter.validateClientFilter();

        assertThatThrownBy(() -> filter.filter(request, nextHandler))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("forbidden client id unknown-service");
    }

    @Test
    void rejectsRequestWithMissingOrMalformedServiceAuthorization() {
        ApplicationParams applicationParams = applicationParams(List.of("/searchCases.*"), List.of());
        SecurityUtils securityUtils = mock(SecurityUtils.class);

        ServerRequest missingHeaderRequest = request(DATA_STORE_PATH, applicationParams, securityUtils);
        var filter = ValidateClientFilter.validateClientFilter();
        HandlerFunction<ServerResponse> nextHandler = nextHandler();
        assertThatThrownBy(() -> filter.filter(missingHeaderRequest, nextHandler))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Missing service authorization token");

        when(securityUtils.getServiceNameFromS2SToken("malformed-token"))
            .thenThrow(new JWTDecodeException("invalid token"));
        ServerRequest malformedHeaderRequest = request(DATA_STORE_PATH, applicationParams, securityUtils,
            "malformed-token");

        assertThatThrownBy(() -> filter.filter(malformedHeaderRequest, nextHandler))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Invalid service authorization token");
    }

    @Test
    void allowsTheServiceConfiguredForTheDefinitionStoreRoute() throws Exception {
        ApplicationParams applicationParams = applicationParams(List.of("/searchCases.*"),
            List.of("/case-types.*"), SERVICE_NAME, DEFINITION_STORE_SERVICE_NAME);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getServiceNameFromS2SToken("definition-store-token"))
            .thenReturn(DEFINITION_STORE_SERVICE_NAME);

        ServerResponse response = ValidateClientFilter.validateClientFilter().filter(
            request(DEFINITION_STORE_PATH, applicationParams, securityUtils, "definition-store-token"),
            nextHandler());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void allowsQuerySpecificDataStoreRoute() throws Exception {
        ApplicationParams applicationParams = applicationParams(List.of("/searchCases\\?ctid=CT_MasterCase"),
            List.of());
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getServiceNameFromS2SToken("incoming-s2s-token")).thenReturn(SERVICE_NAME);

        ServerResponse response = ValidateClientFilter.validateClientFilter().filter(
            request(DATA_STORE_PATH, applicationParams, securityUtils, "incoming-s2s-token"),
            nextHandler());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void rejectsDefinitionStoreServiceOnDataStoreRoute() {
        ApplicationParams applicationParams = applicationParams(List.of("/searchCases.*"),
            List.of("/case-types.*"), SERVICE_NAME, DEFINITION_STORE_SERVICE_NAME);
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getServiceNameFromS2SToken("definition-store-token"))
            .thenReturn(DEFINITION_STORE_SERVICE_NAME);

        assertThatThrownBy(() -> ValidateClientFilter.validateClientFilter().filter(
            request(DATA_STORE_PATH, applicationParams, securityUtils, "definition-store-token"),
            nextHandler()))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("forbidden client id " + DEFINITION_STORE_SERVICE_NAME);
    }

    private ApplicationParams applicationParams(List<String> dataStoreUrls, List<String> definitionStoreUrls) {
        return applicationParams(dataStoreUrls, definitionStoreUrls, SERVICE_NAME, SERVICE_NAME);
    }

    private ApplicationParams applicationParams(List<String> dataStoreUrls, List<String> definitionStoreUrls,
                                                String dataStoreService, String definitionStoreService) {
        ApplicationParams applicationParams = mock(ApplicationParams.class);
        when(applicationParams.getCcdDataStoreAllowedUrls()).thenReturn(dataStoreUrls);
        when(applicationParams.getCcdDefinitionStoreAllowedUrls()).thenReturn(definitionStoreUrls);
        when(applicationParams.getCcdDataStoreAllowedService()).thenReturn(dataStoreService);
        when(applicationParams.getCcdDefinitionStoreAllowedService()).thenReturn(definitionStoreService);
        return applicationParams;
    }

    private ServerRequest request(String path, ApplicationParams applicationParams) {
        return request(path, applicationParams, mock(SecurityUtils.class));
    }

    private ServerRequest request(String path, ApplicationParams applicationParams, SecurityUtils securityUtils) {
        return request(path, applicationParams, securityUtils, null);
    }

    private ServerRequest request(String path, ApplicationParams applicationParams,
                                  SecurityUtils securityUtils, String serviceAuthorization) {
        int queryStart = path.indexOf('?');
        String requestPath = queryStart >= 0 ? path.substring(0, queryStart) : path;
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", requestPath);
        if (queryStart >= 0) {
            servletRequest.setQueryString(path.substring(queryStart + 1));
        }
        if (serviceAuthorization != null) {
            servletRequest.addHeader(SERVICE_AUTHORIZATION, serviceAuthorization);
        }
        WebApplicationContext context = mock(WebApplicationContext.class);
        when(context.getBean(ApplicationParams.class)).thenReturn(applicationParams);
        when(context.getBean(SecurityUtils.class)).thenReturn(securityUtils);
        servletRequest.setAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
        return ServerRequest.create(servletRequest, List.of());
    }

    private HandlerFunction<ServerResponse> nextHandler() {
        return request -> ServerResponse.ok().build();
    }
}
