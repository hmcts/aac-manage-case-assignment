package uk.gov.hmcts.reform.managecase.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonObjectMapperConfig {

    @Primary
    public ObjectMapper objectMapper() {
        return new Jackson2ObjectMapperBuilder()
            .modules(new Jdk8Module(), new JavaTimeModule())
            .build();
    }
}
