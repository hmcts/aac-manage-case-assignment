package uk.gov.hmcts.reform.managecase.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonObjectMapperConfig {

    /**
     * An object mapper configured to support java.time and write Date and Times in ISO8601.
     *
     * @return Default ObjectMapper, used by Spring and HAL to serialise responses, and deserialise requests.
     */
    @Primary
    @Bean(name = "DefaultObjectMapper")
    public ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }

    @Bean(name = "SimpleObjectMapper")
    public ObjectMapper simpleObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
