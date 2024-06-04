package uk.gov.hmcts.reform.managecase.gatewayfilters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.managecase.ApplicationParams;
import uk.gov.hmcts.reform.managecase.api.errorhandling.AccessException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filters requests based on the uri. Checks if it matches against one of the regex list elements defined in
 * ccd.data-store.allowed-urls property.
 */
@Component
public class AllowedUrlRoutingFilter extends AbstractGatewayFilterFactory<OrderedGatewayFilter> {

    @Autowired
    private ApplicationParams applicationParams;

    @Override
    public GatewayFilter apply(OrderedGatewayFilter config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            doValidateAllowedUrls(request);
            return chain.filter(exchange.mutate().request(request).build());
        }, -2);
    }

    private void doValidateAllowedUrls(ServerHttpRequest request) {

        List<String> ccdDataStoreAllowedUrls = applicationParams.getCcdDataStoreAllowedUrls()
            .stream()
            .map("/ccd"::concat)
            .collect(Collectors.toList());

        List<String> ccdDefinitionStoreAllowedUrls = applicationParams.getCcdDefinitionStoreAllowedUrls()
            .stream()
            .map("/ccd"::concat)
            .collect(Collectors.toList());

        String uri = request.getURI().getPath()
            + (request.getURI().getQuery() == null ? "" : "?" + request.getURI().getQuery());

        if (ccdDataStoreAllowedUrls.stream().noneMatch(uri::matches)
            && ccdDefinitionStoreAllowedUrls.stream().noneMatch(uri::matches)) {
            throw new AccessException("Uri not allowed: " + uri);
        }
    }
}
