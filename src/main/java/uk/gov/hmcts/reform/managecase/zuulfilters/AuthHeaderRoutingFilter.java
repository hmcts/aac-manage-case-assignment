package uk.gov.hmcts.reform.managecase.zuulfilters;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.ROUTE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SIMPLE_HOST_ROUTING_FILTER_ORDER;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

/**
 * Adds system user Authorization header with the access token.
 * Adds ServiceAuthorization header with the s2s access token.
 */
@Component
public class AuthHeaderRoutingFilter extends ZuulFilter {
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @Autowired
    private SecurityUtils securityUtils;

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

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();

        addSystemUserHeaders(context);

        return null;
    }

    private void addSystemUserHeaders(RequestContext context) {
        context.addZuulRequestHeader(AUTHORIZATION, securityUtils.getSystemUserToken());
        context.addZuulRequestHeader(SERVICE_AUTHORIZATION, securityUtils.getS2SToken());
    }
}
