package uk.gov.hmcts.reform.managecase.gatewayfilters;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Collection;

import org.springframework.cloud.gateway.server.mvc.common.Shortcut;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.api.errorhandling.AccessException;

import static org.springframework.cloud.gateway.server.mvc.common.MvcUtils.getApplicationContext;

public interface AllowedRoutesFilter {

    @Shortcut
    static HandlerFilterFunction<ServerResponse, ServerResponse> allowedRoutesFilter() {
        return (request, next) -> {

            ApplicationParams applicationParams = getApplicationContext(request).getBean(ApplicationParams.class);

            List<String> ccdDataStoreAllowedUrls = applicationParams.getCcdDataStoreAllowedUrls()
                .stream()
                .map("/ccd"::concat)
                .toList();

            List<String> ccdDefinitionStoreAllowedUrls = applicationParams.getCcdDefinitionStoreAllowedUrls()
                .stream()
                .map("/ccd"::concat)
                .toList();

            String uri = request.uri().getPath()
                + (request.uri().getQuery() == null ? "" : "?" + request.uri().getQuery());

            if (ccdDataStoreAllowedUrls.stream().noneMatch(uri::matches)
                && ccdDefinitionStoreAllowedUrls.stream().noneMatch(uri::matches)) {
                throw new AccessException("Uri not allowed: " + uri);
            }
            
            return next.handle(request);
        };
    }

    class FilterSupplier implements org.springframework.cloud.gateway.server.mvc.filter.FilterSupplier {
        @Override
        public Collection<Method> get() {
            return Arrays.asList(AllowedRoutesFilter.class.getMethods());
        }
    }
    
}
