package uk.gov.hmcts.reform.managecase.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

import java.util.Arrays;

@Configuration
@EnableSwagger2WebMvc
public class SwaggerConfiguration {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .useDefaultResponseMessages(false)
            .select()
            .apis(RequestHandlerSelectors.basePackage(this.getClass().getPackage().getName() + ".api.controller"))
            .paths(PathSelectors.any())
            .build().useDefaultResponseMessages(false)
            .apiInfo(apiInfo())
            .globalOperationParameters(Arrays.asList(headerAuthorization(), headerServiceAuthorization()));
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
            .title("Mange case assignment API")
            .description("Mange case access")
            .version("1.0.0")
            .contact(new Contact("CDM",
                "https://tools.hmcts.net/confluence/display/RCCD/Reform%3A+Core+Case+Data+Home",
                "corecasedatateam@hmcts.net"))
            .termsOfServiceUrl("")
            .build();
    }

    private Parameter headerAuthorization() {
        return new ParameterBuilder()
            .name("Authorization")
            .description("IDAM Bearer token")
            .modelRef(new ModelRef("string"))
            .parameterType("header")
            .required(true)
            .build();
    }

    private Parameter headerServiceAuthorization() {
        return new ParameterBuilder()
            .name("ServiceAuthorization")
            .description("S2S Bearer token of a allowed-list micro-service")
            .modelRef(new ModelRef("string"))
            .parameterType("header")
            .required(true)
            .build();
    }
}
