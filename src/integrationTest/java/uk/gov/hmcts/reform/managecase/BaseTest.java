package uk.gov.hmcts.reform.managecase;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.contract.wiremock.WireMockConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.reform.managecase.fixtures.WiremockFixtures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    void init() {
        Jwt jwt = dummyJwt();
        when(authentication.getPrincipal()).thenReturn(jwt);
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

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    protected void setSecurityAuthorities(String... authorities) {
        Collection<? extends GrantedAuthority> authorityCollection = Stream.of(authorities)
            .map(a -> new SimpleGrantedAuthority(a))
            .collect(Collectors.toCollection(ArrayList::new));
        when(authentication.getAuthorities()).thenAnswer(invocationOnMock -> authorityCollection);
    }

    @Configuration
    static class TestConfiguration {
        @Bean
        public WireMockConfigurationCustomizer wireMockConfigurationCustomizer() {
            return config -> config.extensions(new WiremockFixtures.ConnectionClosedTransformer());
        }
    }

}
