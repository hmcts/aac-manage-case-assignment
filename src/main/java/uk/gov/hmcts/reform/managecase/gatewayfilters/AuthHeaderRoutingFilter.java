package uk.gov.hmcts.reform.managecase.gatewayfilters;

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

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static uk.gov.hmcts.reform.managecase.security.SecurityUtils.SERVICE_AUTHORIZATION;

import java.util.function.Function;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * Validates xui_webapp client_id from ServiceAuthorization header.
 * Adds system user Authorization header with the access token.
 * Adds ServiceAuthorization header with the s2s MCA access token.
 */
@Configuration
public class AuthHeaderRoutingFilter {

    private static final Logger LOG = LoggerFactory.getLogger(AuthHeaderRoutingFilter.class);

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private ApplicationParams applicationParams;

    @Bean
    public RouterFunction<ServerResponse> instrumentRoute() {
		return route().before(validate()).build();
    }

    public Function<ServerRequest, ServerRequest> validate() {
		return request -> {
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
