package uk.gov.hmcts.reform.managecase.client.prd;

import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.managecase.client.AuthHeadersInterceptor;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

public class PrdApiClientConfig {

    @Bean
    public AuthHeadersInterceptor authHeadersInterceptor(SecurityUtils securityUtils) {
        return new AuthHeadersInterceptor(securityUtils);
    }
}
