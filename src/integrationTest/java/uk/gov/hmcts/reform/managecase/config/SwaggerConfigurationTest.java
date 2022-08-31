package uk.gov.hmcts.reform.managecase.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.core.env.Environment;
import uk.gov.hmcts.reform.managecase.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigurationTest extends BaseTest {

    @Autowired
    private WebEndpointsSupplier webEndpointsSupplier;

    @Autowired
    private ServletEndpointsSupplier servletEndpointsSupplier;

    @Autowired
    private ControllerEndpointsSupplier controllerEndpointsSupplier;

    @Autowired
    private EndpointMediaTypes endpointMediaTypes;

    @Autowired
    private CorsEndpointProperties corsProperties;

    @Autowired
    private WebEndpointProperties webEndpointProperties;

    @Autowired
    private Environment environment;

    @Autowired
    private SwaggerConfiguration swaggerConfiguration;

    @Test
    public void successfullyLoadWebEndpointServletHandlerMappingTest() {
        assertThat(webEndpointsSupplier).isNotNull();
        assertThat(servletEndpointsSupplier).isNotNull();
        assertThat(controllerEndpointsSupplier).isNotNull();
        assertThat(endpointMediaTypes).isNotNull();
        assertThat(corsProperties).isNotNull();
        assertThat(webEndpointProperties).isNotNull();
        assertThat(environment).isNotNull();
        assertThat(swaggerConfiguration).isNotNull();

        assertThat(swaggerConfiguration.webEndpointServletHandlerMapping(webEndpointsSupplier, servletEndpointsSupplier,
            controllerEndpointsSupplier, endpointMediaTypes,corsProperties, webEndpointProperties, environment))
            .satisfies(mapping -> {
                assertThat(mapping.getEndpoints().size()).isEqualTo(2);
                assertThat(mapping.getApplicationContext().getId()).isEqualTo("application-1");
            });
    }

}