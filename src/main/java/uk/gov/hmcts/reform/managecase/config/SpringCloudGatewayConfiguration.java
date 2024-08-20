package uk.gov.hmcts.reform.managecase.config;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.api.errorhandling.AccessException;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.web.servlet.function.RequestPredicates.path;
import static org.springframework.web.servlet.function.RouterFunctions.route;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

@Configuration
public class SpringCloudGatewayConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SpringCloudGatewayConfiguration.class);

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    ApplicationParams applicationParams;

    @Bean
    public RouterFunction<ServerResponse> allowedRouterFunction() {
        return route()
            .before(validateAllowedUrls())
            .before(validateClientId())
            .route(path("/ccd/**"), http("http://localhost:8080")) 
            .build();
    }

    public Function<ServerRequest, ServerRequest> validateAllowedUrls() {
        return (request) -> {
            List<String> ccdDataStoreAllowedUrls = applicationParams.getCcdDataStoreAllowedUrls()
                .stream()
                .map("/ccd"::concat)
                .collect(Collectors.toList());

            List<String> ccdDefinitionStoreAllowedUrls = applicationParams.getCcdDefinitionStoreAllowedUrls()
                .stream()
                .map("/ccd"::concat)
                .collect(Collectors.toList());

            String uri = request.path()
                + (request.uri().getQuery() == null ? "" : "?" + request.uri().getQuery());

            if (ccdDataStoreAllowedUrls.stream().noneMatch(uri::matches)
                && ccdDefinitionStoreAllowedUrls.stream().noneMatch(uri::matches)) {
                throw new AccessException("Uri not allowed: " + uri);
            }
            return ServerRequest.from(request).build();
        };
    }

    public Function<ServerRequest, ServerRequest> validateClientId() {
        return (request) -> {
            doValidateClientId(request);
            return ServerRequest
            .from(request)
            .header(AUTHORIZATION, securityUtils.getCaaSystemUserToken())
            .header(SERVICE_AUTHORIZATION, securityUtils.getS2SToken())
            .build();
        };
    }

    private void doValidateClientId(ServerRequest request) {
        String serviceName = securityUtils
                .getServiceNameFromS2SToken(request.headers().firstHeader(SERVICE_AUTHORIZATION));

        if (!applicationParams.getCcdDataStoreAllowedService().equals(serviceName)
            || !applicationParams.getCcdDefinitionStoreAllowedService().equals(serviceName)) {
            String errorMessage = String.format("forbidden client id %s for the /ccd endpoint", serviceName);
            LOG.debug(errorMessage);
            throw new AccessException(errorMessage);
        }
    }
    
}
