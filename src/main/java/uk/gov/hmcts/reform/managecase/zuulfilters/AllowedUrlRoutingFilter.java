package uk.gov.hmcts.reform.managecase.zuulfilters;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.managecase.ApplicationParams;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SIMPLE_HOST_ROUTING_FILTER_ORDER;

/**
 * Filters requests based on the uri. Checks if it matches against one of the regex list elements defined in
 * ccd.data-store.allowed-urls property.
 */
@Component
public class AllowedUrlRoutingFilter extends ZuulFilter {

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

        validateAllowedUrls(context);

        return null;
    }

    private void validateAllowedUrls(RequestContext context) {

        List<String> ccdDataStoreAllowedUrls = applicationParams.getCcdDataStoreAllowedUrls()
            .stream()
            .map("/ccd"::concat)
            .collect(Collectors.toList());

        List<String> ccdDefinitionStoreAllowedUrls = applicationParams.getCcdDefinitionStoreAllowedUrls()
            .stream()
            .map("/ccd"::concat)
            .collect(Collectors.toList());

        List<String> prdAllowedUrls = applicationParams.getPrdAllowedUrls()
            .stream()
            .map("/prd"::concat)
            .collect(Collectors.toList());

        String uri = context.getRequest().getRequestURI()
            + (context.getRequest().getQueryString() == null ? "" : "?" + context.getRequest().getQueryString());

        if (ccdDataStoreAllowedUrls.stream().noneMatch(uri::matches)
            && ccdDefinitionStoreAllowedUrls.stream().noneMatch(uri::matches)
            && prdAllowedUrls.stream().noneMatch(uri::matches)) {
            ZuulException zuulException = new ZuulException("Uri not allowed: " + uri, 403, "Uri not allowed");
            throw new ZuulRuntimeException(zuulException);
        }
    }
}
