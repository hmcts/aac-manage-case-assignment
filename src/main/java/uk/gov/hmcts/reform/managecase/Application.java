package uk.gov.hmcts.reform.managecase;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableFeignClients
@EnableWebFlux
public class Application {

    public static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class)
            .web(WebApplicationType.REACTIVE)
            .run(args);
    }
}
