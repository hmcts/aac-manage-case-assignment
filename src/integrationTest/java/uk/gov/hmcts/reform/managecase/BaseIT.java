package uk.gov.hmcts.reform.managecase;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.bouncycastle.jcajce.provider.asymmetric.dsa.DSASigner.detDSA;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures;

@SpringBootTest(classes = {
    Application.class,
    TestIdamConfiguration.class
})
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0, stubs = "classpath:/wiremock-stubs")
@TestPropertySource("/integration-test.yaml")
public class BaseIT {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Value("${wiremock.server.port}")
    protected Integer wiremockPort;

    @Mock
    protected Authentication authentication;

    @MockBean
    protected JwtDecoder dummyDecoder;

    @BeforeEach
    void setupDecoder() {
        Jwt dummyJwt = dummyJwt();
        when(dummyDecoder.decode(anyString())).thenReturn(dummyJwt);
        when(authentication.getPrincipal()).thenReturn(dummyJwt);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
    }
    
    @Configuration
    static class WireMockTestConfiguration {
        @Bean
        public WireMockConfigurationCustomizer wireMockConfigurationCustomizer() {
            return config -> config.extensions(new WiremockFixtures.ConnectionClosedTransformer());
        }
    }
    
    private Jwt dummyJwt() {
        return Jwt.withTokenValue("a dummy jwt token")
                .claim("aClaim", "aClaim")
                .header("aHeader", "aHeader")
                .build();
    }
}
