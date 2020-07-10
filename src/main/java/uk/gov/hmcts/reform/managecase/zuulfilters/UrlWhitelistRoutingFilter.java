package uk.gov.hmcts.reform.managecase.zuulfilters;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.managecase.ApplicationParams;

import java.util.List;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SIMPLE_HOST_ROUTING_FILTER_ORDER;

@Component
public class UrlWhitelistRoutingFilter extends ZuulFilter {

    @Autowired
    private ApplicationParams applicationParams;

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return SIMPLE_HOST_ROUTING_FILTER_ORDER - 2;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() {
        RequestContext context = RequestContext.getCurrentContext();

        validateWhitelistedUrls(context);

        return null;
    }

    private void validateWhitelistedUrls(RequestContext context) {

        List<String> ccdDataStoreWhitelistedUrls = applicationParams.getCcdDataStoreWhitelistedUrls();

        String uri = context.getRequest().getRequestURI()
            + (context.getRequest().getQueryString() == null ? "" : "?" + context.getRequest().getQueryString());

        if (ccdDataStoreWhitelistedUrls.stream().noneMatch(uri::matches)) {
            ZuulException zuulException = new ZuulException("Url not whitelisted: " + uri, 403, "Url not whitelisted");
            throw new ZuulRuntimeException(zuulException);
        }
    }
}
