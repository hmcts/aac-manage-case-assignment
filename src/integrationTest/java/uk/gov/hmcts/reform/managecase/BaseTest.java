package uk.gov.hmcts.reform.managecase;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
    Application.class,
    TestIdamConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureWireMock(port = 0, stubs = "classpath:/wiremock-stubs")
@TestPropertySource("/integration-test.properties")
public class BaseTest {

    @Value("${wiremock.server.port}")
    protected Integer wiremockPort;
    @Autowired
    protected ApplicationParams applicationParams;
    @Mock
    protected Authentication authentication;

    @BeforeEach
    void setUp() {
        Jwt jwt = dummyJwt();
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }

    private Jwt dummyJwt() {
        return Jwt.withTokenValue("Bearer a jwt token")
                .claim("aClaim", "aClaim")
                .header("aHeader", "aHeader")
                .build();
    }

}
