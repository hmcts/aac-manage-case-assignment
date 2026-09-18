package uk.gov.hmcts.reform.managecase.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonObjectMapperConfigTest {

    @Test
    void shouldConfigureDefaultMapperForApiPayloads() {
        ObjectMapper mapper = new JacksonObjectMapperConfig().defaultObjectMapper();

        assertThat(mapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
        assertThat(mapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();
        assertThat(mapper.getFactory().isEnabled(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION))
            .isTrue();
    }

    @Test
    void shouldCreateSimpleMapper() {
        assertThat(new JacksonObjectMapperConfig().simpleObjectMapper()).isNotNull();
    }
}
