package uk.gov.hmcts.reform.managecase.zuulfilters;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SIMPLE_HOST_ROUTING_FILTER_ORDER;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Validates xui_webapp client_id from ServiceAuthorization header.
 * Adds system user Authorization header with the access token.
 * Adds ServiceAuthorization header with the s2s MCA access token.
 */
@Component
public class AuthHeaderRoutingFilter extends ZuulFilter {
    private static final Logger LOG = LoggerFactory.getLogger(AuthHeaderRoutingFilter.class);

    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    private final SecurityUtils securityUtils;
    private final ApplicationParams applicationParams;

    @Override
    public String filterType() {
        return ROUTE_TYPE;
    }

    @Override
    public int filterOrder() {
        return SIMPLE_HOST_ROUTING_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    public AuthHeaderRoutingFilter(SecurityUtils securityUtils,
                                   ApplicationParams applicationParams) {
        super();
        this.securityUtils = securityUtils;
        this.applicationParams = applicationParams;
    }

    @SneakyThrows
    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();

        validateClientId(context);
        addSystemUserHeaders(context);

        return null;
    }

    private void validateClientId(RequestContext context) {
        String serviceName = securityUtils
                .getServiceNameFromS2SToken(context.getRequest().getHeader(SERVICE_AUTHORIZATION));

        if (!applicationParams.getCcdDataStoreAllowedService().equals(serviceName)
            || !applicationParams.getCcdDefinitionStoreAllowedService().equals(serviceName)
            || !applicationParams.getPrdAllowedService().equals(serviceName)) {
            String errorMessage = String.format("forbidden client id %s for the endpoint", serviceName);
            LOG.debug(errorMessage);

            ZuulException zuulException = new ZuulException(errorMessage, 403, errorMessage);
            throw new ZuulRuntimeException(zuulException);
        }
    }

    private void addSystemUserHeaders(RequestContext context) {
        context.addZuulRequestHeader(AUTHORIZATION, securityUtils.getSystemUserToken());
        context.addZuulRequestHeader(SERVICE_AUTHORIZATION, securityUtils.getS2SToken());
    }
}
