package uk.gov.hmcts.reform.managecase.gatewayfilters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.api.errorhandling.AccessException;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Validates xui_webapp client_id from ServiceAuthorization header.
 * Adds system user Authorization header with the access token.
 * Adds ServiceAuthorization header with the s2s MCA access token.
 */
@Component
public class AuthHeaderRoutingFilter extends AbstractGatewayFilterFactory<OrderedGatewayFilter> {
    private static final Logger LOG = LoggerFactory.getLogger(AuthHeaderRoutingFilter.class);

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final SecurityUtils securityUtils;
    private final ApplicationParams applicationParams;

    public AuthHeaderRoutingFilter(SecurityUtils securityUtils,
                                   ApplicationParams applicationParams) {
        super();
        this.securityUtils = securityUtils;
        this.applicationParams = applicationParams;
    }

    @Override
    public GatewayFilter apply(OrderedGatewayFilter config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            doValidateClientId(request);
            request = request.mutate().headers((httpHeaders) -> {
                httpHeaders.add(AUTHORIZATION, securityUtils.getCaaSystemUserToken());
                httpHeaders.add(SERVICE_AUTHORIZATION, securityUtils.getS2SToken());
            }).build();
            return chain.filter(exchange.mutate().request(request).build());
        }, -1);
    }

    private void doValidateClientId(ServerHttpRequest request) {
        String serviceName = securityUtils
                .getServiceNameFromS2SToken(request.getHeaders().get(SERVICE_AUTHORIZATION).get(0));

        if (!applicationParams.getCcdDataStoreAllowedService().equals(serviceName)
            || !applicationParams.getCcdDefinitionStoreAllowedService().equals(serviceName)) {
            String errorMessage = String.format("forbidden client id %s for the /ccd endpoint", serviceName);
            LOG.debug(errorMessage);
            throw new AccessException(errorMessage);
        }
    }
}
