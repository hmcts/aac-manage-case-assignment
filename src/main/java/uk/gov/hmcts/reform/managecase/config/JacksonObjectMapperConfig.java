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
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.lang.reflect.Constructor;

@Configuration
public class JacksonObjectMapperConfig implements WebMvcConfigurer {

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

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.configureMessageConvertersList(converters -> {
            converters.removeIf(JacksonJsonHttpMessageConverter.class::isInstance);
            if (converters.stream().noneMatch(this::isJackson2Converter)) {
                converters.add(createJackson2Converter());
            }
        });
    }

    /**
     * Keep the Jackson 2 converter until the application DTOs and HMCTS clients are migrated to Jackson 3.
     * Reflection isolates this temporary bridge from the deprecated Spring converter API.
     */
    HttpMessageConverter<?> createJackson2Converter() {
        return createJackson2Converter(jackson2ConverterClass());
    }

    HttpMessageConverter<?> createJackson2Converter(Class<?> converterClass) {
        try {
            Constructor<?> constructor = converterClass.getConstructor(ObjectMapper.class);
            return (HttpMessageConverter<?>) constructor.newInstance(defaultObjectMapper());
        } catch (ReflectiveOperationException | ClassCastException exception) {
            throw new IllegalStateException("Jackson 2 HTTP converter is unavailable", exception);
        }
    }

    boolean isJackson2Converter(HttpMessageConverter<?> converter) {
        return jackson2ConverterClass().isInstance(converter);
    }

    Class<?> jackson2ConverterClass() {
        return jackson2ConverterClass("org.springframework.http.converter.json.MappingJackson2HttpMessageConverter");
    }

    Class<?> jackson2ConverterClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Jackson 2 HTTP converter is unavailable", exception);
        }
    }

}
