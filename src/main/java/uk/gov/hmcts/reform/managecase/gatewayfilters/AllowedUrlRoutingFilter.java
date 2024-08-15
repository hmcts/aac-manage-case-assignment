package uk.gov.hmcts.reform.managecase.gatewayfilters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.api.errorhandling.AccessException;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Filters requests based on the uri. Checks if it matches against one of the regex list elements defined in
 * ccd.data-store.allowed-urls property.
 */
@Configuration
public class AllowedUrlRoutingFilter {

    @Autowired
    ApplicationParams applicationParams;

    @Bean
    public RouterFunction<ServerResponse> instrumentRoute() {
		return route().before(validate()).build();
    }

    public Function<ServerRequest, ServerRequest> validate() {
		return request -> {
            doValidateAllowedUrls(request);
            return ServerRequest.from(request).build();
        };
	}

    private void doValidateAllowedUrls(ServerRequest request) {

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
    }
}
