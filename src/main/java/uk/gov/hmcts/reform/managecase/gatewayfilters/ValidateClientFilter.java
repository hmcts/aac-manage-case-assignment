package uk.gov.hmcts.reform.managecase.gatewayfilters;

import com.auth0.jwt.exceptions.JWTDecodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.server.mvc.common.Shortcut;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import static org.springframework.cloud.gateway.server.mvc.common.MvcUtils.getApplicationContext;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

public interface ValidateClientFilter {

    String X_FORWARDED_PREFIX = "X-Forwarded-Prefix";

    @Shortcut
    static HandlerFilterFunction<ServerResponse, ServerResponse> validateClientFilter() {
        return (request, next) -> {
            Logger log = LoggerFactory.getLogger(ValidateClientFilter.class);
            SecurityUtils securityUtils = getApplicationContext(request).getBean(SecurityUtils.class);
            ApplicationParams applicationParams = getApplicationContext(request).getBean(ApplicationParams.class);

            String serviceAuthorization = request.headers().firstHeader(SERVICE_AUTHORIZATION);
            if (serviceAuthorization == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing service authorization token");
            }

            String service;
            try {
                service = securityUtils.getServiceNameFromS2SToken(serviceAuthorization);
            } catch (JWTDecodeException exception) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid service authorization token");
            }
            String allowedService = allowedServiceForRoute(request, applicationParams);
            if (allowedService == null || !allowedService.equals(service)) {
                String errorMessage = String.format("forbidden client id %s for the /ccd endpoint", service);
                log.debug(errorMessage);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, errorMessage);
            }

            ServerRequest withHeaders = ServerRequest
                .from(request)
                .headers(headers -> headers.set(X_FORWARDED_PREFIX, "/ccd"))
                .headers(headers -> {
                    headers.remove(AUTHORIZATION);
                    headers.remove(SERVICE_AUTHORIZATION);
                    headers.set(AUTHORIZATION, securityUtils.getCaaSystemUserToken());
                    headers.set(SERVICE_AUTHORIZATION, securityUtils.getS2SToken());
                })
                .build();
            return next.handle(withHeaders);
        };
    }

    static String allowedServiceForRoute(ServerRequest request, ApplicationParams applicationParams) {
        String requestUri = request.uri().getPath()
            + (request.uri().getQuery() == null ? "" : "?" + request.uri().getQuery());
        boolean dataStoreRoute = matchesConfiguredRoute(requestUri,
            applicationParams.getCcdDataStoreAllowedUrls());
        boolean definitionStoreRoute = matchesConfiguredRoute(requestUri,
            applicationParams.getCcdDefinitionStoreAllowedUrls());

        if (dataStoreRoute && definitionStoreRoute) {
            String dataStoreService = applicationParams.getCcdDataStoreAllowedService();
            String definitionStoreService = applicationParams.getCcdDefinitionStoreAllowedService();
            return Objects.equals(dataStoreService, definitionStoreService) ? dataStoreService : null;
        }
        if (!dataStoreRoute) {
            return null;
        }
        return applicationParams.getCcdDataStoreAllowedService();
    }

    static boolean matchesConfiguredRoute(String requestUri, List<String> allowedUrls) {
        return allowedUrls.stream()
            .map("/ccd"::concat)
            .anyMatch(requestUri::matches);
    }

    class FilterSupplier implements org.springframework.cloud.gateway.server.mvc.filter.FilterSupplier {
        @Override
        public Collection<Method> get() {
            return Arrays.stream(ValidateClientFilter.class.getMethods())
                .filter(method -> method.isAnnotationPresent(Shortcut.class))
                .toList();
        }
    }

}
