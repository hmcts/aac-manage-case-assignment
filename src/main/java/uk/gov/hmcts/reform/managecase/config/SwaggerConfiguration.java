package uk.gov.hmcts.reform.managecase.config;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(apiInfo());
    }

    @Bean
    public OperationCustomizer customGlobalHeaders() {
        return (Operation customOperation, HandlerMethod handlerMethod) -> {
            customOperation.addParametersItem(headerServiceAuthorization());
            customOperation.addParametersItem(headerAuthorization());
            return customOperation;
        };
    }

    private Info apiInfo() {
        return new Info()
                .title("Mange case assignment API")
                .description("Mange case access")
                .version("1.0.0")
                .contact(new Contact().name("CDM")
                        .url("https://tools.hmcts.net/confluence/display/RCCD/Reform%3A+Core+Case+Data+Home")
                        .email("corecasedatateam@hmcts.net"));
    }

    private Parameter headerServiceAuthorization() {
        return new Parameter()
                .in(ParameterIn.HEADER.toString())
                .schema(new StringSchema())
                .name("ServiceAuthorization")
                .description("Valid Service-to-Service JWT token for a whitelisted micro-service")
                .required(true);
    }

    private Parameter headerAuthorization() {
        return new Parameter()
                .in(ParameterIn.HEADER.toString())
                .schema(new StringSchema())
                .name("Authorization")
                .description("Keyword `Bearer` followed by a valid IDAM user token")
                .required(true);
    }
}
