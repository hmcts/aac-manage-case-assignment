package uk.gov.hmcts.reform.managecase.client.prd;

import feign.Client;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;

public class PrdFeignClient extends Client.Default {
    private static final Logger log = LoggerFactory.getLogger(PrdFeignClient.class);

    public PrdFeignClient(SSLSocketFactory sslContextFactory, HostnameVerifier hostnameVerifier) {
        super(sslContextFactory, hostnameVerifier);
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        Response response = super.execute(request, options);
        return checkResponse(response);
    }

    public Response checkResponse(Response response) throws IOException {
        byte[] bytes = null;

        if (response.body() != null) {
            InputStream is = response.body().asInputStream();
            bytes = IOUtils.toByteArray(is);
        }

        long responseLength = bytes != null ? bytes.length : 0;

        if (response.status() == 200 && responseLength == 0) {
            log.error("Received empty response from API with 200 status code. "
                         + "Throwing RetryableException to retry the api call.");

            throw new RetryableException(
                502,
                response.reason(),
                response.request().httpMethod(),
                (Long) null,
                response.request()
            );
        }

        return response.toBuilder().body(bytes).build();
    }
}
