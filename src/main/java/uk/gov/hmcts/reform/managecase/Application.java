package uk.gov.hmcts.reform.managecase;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication
@EnableFeignClients
@EnableWebMvc
public class Application {

    public static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class)
            .web(WebApplicationType.SERVLET)
            .run(args);
    }
}
