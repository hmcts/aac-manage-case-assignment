package uk.gov.hmcts.reform.managecase;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringBootApplication(scanBasePackages = {
    "uk.gov.hmcts.reform.managecase", 
    "uk.gov.hmcts.reform.idam.client",
    "uk.gov.hmcts.reform.authorisation"
})
@EnableFeignClients
@EnableWebMvc
public class Application {

    @Bean
    public ServerCodecConfigurer serverCodecConfigurer() {
        return ServerCodecConfigurer.create();
    }

    public static void main(final String[] args) {
        new SpringApplicationBuilder(Application.class)
            .web(WebApplicationType.SERVLET)
            .run(args);
    }
}
