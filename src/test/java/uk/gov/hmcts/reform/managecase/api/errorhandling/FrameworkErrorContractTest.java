package uk.gov.hmcts.reform.managecase.api.errorhandling;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;

class FrameworkErrorContractTest {
    @RestController
    static class Probe {
        @GetMapping("/review-probe")
        String probe(@RequestParam("id") Long id) {
            return "ok";
        }
    }

    @Test
    void typeMismatchKeepsFrameworkSchemaWithoutRejectedValues() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new Probe()).setControllerAdvice(new RestExceptionHandler()).build();
        var response = mvc.perform(get("/review-probe").param("id", "invalid")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("Bad Request")
            .doesNotContain("invalid", "java.lang", "review-probe", "\"code\"");
    }

    @Test
    void methodNotAllowedKeepsStatusAndFrameworkSchema() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new Probe()).setControllerAdvice(new RestExceptionHandler()).build();
        var response = mvc.perform(post("/review-probe")).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(405);
        assertThat(response.getContentAsString()).contains("Method Not Allowed")
            .doesNotContain("review-probe", "\"message\"", "\"code\"");
        assertThat(response.getHeader("Allow")).contains("GET");
    }
}
