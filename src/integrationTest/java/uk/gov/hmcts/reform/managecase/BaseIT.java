package uk.gov.hmcts.reform.managecase;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("integration")
@SpringBootTest(classes = {
    Application.class,
    TestIdamConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureWireMock(port = 0, stubs = "classpath:/wiremock-stubs")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
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
    
    private Jwt dummyJwt() {
        return Jwt.withTokenValue("a dummy jwt token")
                .claim("aClaim", "aClaim")
                .header("aHeader", "aHeader")
                .build();
    }

}
