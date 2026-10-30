package uk.gov.hmcts.reform.managecase.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.Decoder;
import feign.optionals.OptionalDecoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

public final class DownstreamResponseDecoderFactory {

    private DownstreamResponseDecoderFactory() {
    }

    public static Decoder tolerantJsonDecoder(ObjectMapper objectMapper) {
        ObjectMapper downstreamObjectMapper = objectMapper.copy()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        MappingJackson2HttpMessageConverter jsonConverter =
            new MappingJackson2HttpMessageConverter(downstreamObjectMapper);
        ObjectFactory<HttpMessageConverters> messageConverters =
            () -> new HttpMessageConverters(jsonConverter);

        return new OptionalDecoder(new ResponseEntityDecoder(new SpringDecoder(messageConverters)));
    }
}
