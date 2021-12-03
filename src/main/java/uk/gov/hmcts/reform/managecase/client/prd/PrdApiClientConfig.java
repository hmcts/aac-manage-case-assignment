package uk.gov.hmcts.reform.managecase.client.prd;

import feign.Client;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.managecase.client.AuthHeadersInterceptor;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

public class PrdApiClientConfig {

    @Bean
    public AuthHeadersInterceptor authHeadersInterceptor(SecurityUtils securityUtils) {
        return new AuthHeadersInterceptor(securityUtils);
    }

    @Bean
    public Client client() {
        return new PrdFeignClient(null, null);
    }

    @Bean
    public Retryer retryer(@Value("${prd.client.retryer.period}") long period,
                           @Value("${prd.client.retryer.maxPeriod}") long maxPeriod,
                           @Value("${prd.client.retryer.maxAttempts}") int maxAttempts) {
        return new Retryer.Default(period, maxPeriod, maxAttempts);
    }
}
