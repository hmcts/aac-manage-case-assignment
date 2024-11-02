package uk.gov.hmcts.reform.managecase.config;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
public class RestTemplateConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(RestTemplateConfiguration.class);

    private PoolingHttpClientConnectionManager cm;

    @Value("${http.client.max.total}")
    private int maxTotalHttpClient;

    @Value("${http.client.seconds.idle.connection}")
    private int maxSecondsIdleConnection;

    @Value("${http.client.max.client_per_route}")
    private int maxClientPerRoute;

    @Value("${http.client.validate.after.inactivity}")
    private int validateAfterInactivity;

    @Value("${http.client.connection.timeout}")
    private int connectionTimeout;

    @Value("${http.client.read.timeout}")
    private int readTimeout;

    @Primary
    @Bean
    CloseableHttpClient getCloseableHttpClient() {
        PoolingHttpClientConnectionManagerBuilder builder = PoolingHttpClientConnectionManagerBuilder.create();
        builder.setConnectionConfigResolver((HttpRoute object) -> ConnectionConfig.custom()
            .setTimeToLive(maxSecondsIdleConnection, TimeUnit.SECONDS)
            .setConnectTimeout(readTimeout, TimeUnit.SECONDS)
            .setSocketTimeout(readTimeout, TimeUnit.SECONDS)
            .setValidateAfterInactivity(validateAfterInactivity, TimeUnit.SECONDS)
            .build());
        RequestConfig config = RequestConfig.custom()
            .setConnectionRequestTimeout(readTimeout, TimeUnit.SECONDS)
            .build();

        return HttpClientBuilder.create()
            .useSystemProperties()
            .disableRedirectHandling()
            .setDefaultRequestConfig(config)
            .setConnectionManager(builder.build())
            .build();
    }

    @Bean(name = "restTemplate")
    public RestTemplate restTemplate() {
        final var restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(getHttpClient()));
        return restTemplate;
    }

    private HttpClient getHttpClient() {
        return getHttpClient(connectionTimeout);
    }

    private HttpClient getHttpClient(final int timeout) {

        LOG.info("maxTotalHttpClient: {}", maxTotalHttpClient);
        LOG.info("maxSecondsIdleConnection: {}", maxSecondsIdleConnection);
        LOG.info("maxClientPerRoute: {}", maxClientPerRoute);
        LOG.info("validateAfterInactivity: {}", validateAfterInactivity);
        LOG.info("connectionTimeout: {}", timeout);
        LOG.info("readTimeout: {}", readTimeout);

        PoolingHttpClientConnectionManagerBuilder builder = PoolingHttpClientConnectionManagerBuilder.create();

        builder.setMaxConnTotal(maxTotalHttpClient);
        builder.setMaxConnPerRoute(maxClientPerRoute);
        builder.setConnectionConfigResolver((HttpRoute object) -> ConnectionConfig.custom()
            .setTimeToLive(maxSecondsIdleConnection, TimeUnit.SECONDS)
            .setConnectTimeout(timeout, TimeUnit.SECONDS)
            .setSocketTimeout(timeout, TimeUnit.SECONDS)
            .setValidateAfterInactivity(validateAfterInactivity, TimeUnit.SECONDS)
            .build());
        builder.setSocketConfigResolver((HttpRoute object) -> SocketConfig.custom()
            .setSoTimeout(readTimeout, TimeUnit.SECONDS)
            .build());

        cm = builder.build();

        cm.closeIdle(TimeValue.of(maxSecondsIdleConnection, TimeUnit.SECONDS));
        
        final RequestConfig config = RequestConfig.custom()
            .setConnectionRequestTimeout(timeout, TimeUnit.SECONDS)
            .build();

        return HttpClientBuilder.create()
            .useSystemProperties()
            .setDefaultRequestConfig(config)
            .setConnectionManager(cm)
            .build();
    }
}
