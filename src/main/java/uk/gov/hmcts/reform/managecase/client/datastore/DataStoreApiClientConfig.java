package uk.gov.hmcts.reform.managecase.client.datastore;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import feign.Retryer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import uk.gov.hmcts.reform.managecase.client.DownstreamResponseDecoderFactory;
import uk.gov.hmcts.reform.managecase.client.SystemUserAuthHeadersInterceptor;
import uk.gov.hmcts.reform.managecase.security.SecurityUtils;

public class DataStoreApiClientConfig {

    @Bean
    public SystemUserAuthHeadersInterceptor systemUserAuthHeadersInterceptor(SecurityUtils securityUtils) {
        return new SystemUserAuthHeadersInterceptor(securityUtils);
    }

    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY;
    }

    @Bean
    public Decoder dataStoreResponseDecoder(@Qualifier("DefaultObjectMapper") ObjectMapper objectMapper) {
        return DownstreamResponseDecoderFactory.tolerantJsonDecoder(objectMapper);
    }
}
