package uk.gov.hmcts.reform.managecase.client.datastore;

import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

public class DataStoreApiClientConfig {

    @Bean
    public AuthHeadersInterceptor authHeadersInterceptor(SecurityUtils securityUtils) {
        return new AuthHeadersInterceptor(securityUtils);
    }
}
