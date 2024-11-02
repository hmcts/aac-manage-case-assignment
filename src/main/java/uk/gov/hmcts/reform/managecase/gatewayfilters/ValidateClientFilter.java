package uk.gov.hmcts.reform.managecase.gatewayfilters;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.server.mvc.common.Shortcut;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.api.errorhandling.AccessException;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import static org.springframework.cloud.gateway.server.mvc.common.MvcUtils.getApplicationContext;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

public interface ValidateClientFilter {

    @Shortcut
    static HandlerFilterFunction<ServerResponse, ServerResponse> validateClientFilter() {
        return (request, next) -> {
            Logger log = LoggerFactory.getLogger(ValidateClientFilter.class);
            SecurityUtils securityUtils = getApplicationContext(request).getBean(SecurityUtils.class);
            ApplicationParams applicationParams = getApplicationContext(request).getBean(ApplicationParams.class);

            String service = securityUtils.getServiceNameFromS2SToken(
                request.headers().firstHeader(SERVICE_AUTHORIZATION)
            );
            if (!applicationParams.getCcdDataStoreAllowedService().equals(service)
                    || !applicationParams.getCcdDefinitionStoreAllowedService().equals(service)) {
                String errorMessage = String.format("forbidden client id %s for the /ccd endpoint", service);
                log.debug(errorMessage);
                throw new AccessException(errorMessage);
            }

            ServerRequest withHeaders = ServerRequest
                .from(request)
                .header(AUTHORIZATION, securityUtils.getCaaSystemUserToken())
                .header(SERVICE_AUTHORIZATION, securityUtils.getS2SToken())
                .build();
            return next.handle(withHeaders);
        };
    }

    class FilterSupplier implements org.springframework.cloud.gateway.server.mvc.filter.FilterSupplier {
        @Override
        public Collection<Method> get() {
            return Arrays.asList(ValidateClientFilter.class.getMethods());
        }
    }

}
