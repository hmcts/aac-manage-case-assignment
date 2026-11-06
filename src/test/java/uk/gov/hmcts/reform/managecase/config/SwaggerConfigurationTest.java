package uk.gov.hmcts.reform.managecase.config;

import io.swagger.v3.oas.models.Operation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigurationTest {

    @Test
    void shouldExposeApiMetadata() {
        assertThat(new SwaggerConfiguration().openAPI().getInfo().getTitle())
            .isEqualTo("Mange case assignment API");
    }

    @Test
    void shouldAddAuthenticationHeadersToOperations() {
        Operation operation = new Operation();

        new SwaggerConfiguration().customGlobalHeaders().customize(operation, null);

        assertThat(operation.getParameters()).extracting("name")
            .containsExactly("ServiceAuthorization", "Authorization");
    }
}
