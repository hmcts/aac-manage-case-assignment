package uk.gov.hmcts.reform.managecase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

@SpringBootTest(classes = {
    Application.class,
    TestIdamConfiguration.class
})
@SpringJUnitWebConfig
@AutoConfigureMockMvc
@TestPropertySource("/integration-test.properties")
@AutoConfigureWireMock(port = 0, stubs = "classpath:/wiremock-stubs")
public class BaseTest {

    @Value("${wiremock.server.port}")
    protected Integer wiremockPort;
}
