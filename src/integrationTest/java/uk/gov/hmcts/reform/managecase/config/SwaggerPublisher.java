package uk.gov.hmcts.reform.managecase.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;
import uk.gov.hmcts.reform.managecase.BaseIT;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Built-in feature which saves service's swagger specs in temporary directory.
 * Each travis run on master should automatically save and upload (if updated) documentation.
 */
class SwaggerPublisher extends BaseIT {

    @Autowired
    protected WebTestClient webClient;

    @DisplayName("Generate swagger documentation")
    @Test
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
    void generateDocs() throws Exception {
        
        byte[] specs = webClient.get().uri("/v3/api-docs")
            .exchange()
                .expectStatus().isOk()
            .expectBody()
                .returnResult().getResponseBody();

        try (OutputStream outputStream = Files.newOutputStream(Paths.get("/tmp/swagger-specs.json"))) {
            outputStream.write(specs);
        }

    }
}
