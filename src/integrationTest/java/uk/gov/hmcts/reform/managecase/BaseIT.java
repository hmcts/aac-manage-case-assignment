package uk.gov.hmcts.reform.managecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.ConfigureWireMock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@ActiveProfiles("integration")
@SpringBootTest(classes = {
    Application.class
})
@AutoConfigureMockMvc(addFilters = false)
@EnableWireMock(@ConfigureWireMock(filesUnderClasspath = "wiremock-stubs"))
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

    @MockitoBean
    protected JwtDecoder dummyDecoder;

    @BeforeEach
    void setupDecoder() {
        Jwt dummyJwt = dummyJwt();
        when(dummyDecoder.decode(anyString())).thenReturn(dummyJwt);
        lenient().when(authentication.getPrincipal()).thenReturn(dummyJwt);
        SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

        // WireMock's Boot 4 integration does not recursively load the legacy
        // Spring Cloud Contract mapping directory, so keep the S2S lease stub
        // explicit while the remaining fixtures are migrated.
        WireMock.configureFor("localhost", wiremockPort);
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/s2s/lease"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjY2RfZ3ci"
                    + "LCJleHAiOjE1ODM0NDUyOTd9.WWRzROlKxLQCJw5h0h0dHb9hHfbBhF2Idwv1z4L4FnqSw3VZ38ZRLuDmwr3tj-"
                    + "8oOv6EfLAxV0dJAPtUT203Iw")));
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/o/token"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"access_token\":\"eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODM0NDUyOTd9aa\"}")));
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/o/token"))
            .withRequestBody(WireMock.containing("noc.approver%40gmail.com"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"access_token\":\"eyJzdWIiOiJjY6ShZ3ciLCJleHAiOlD1OEM0NDUyOTd7co\"}")));
        WireMock.stubFor(WireMock.post(WireMock.urlEqualTo("/o/token"))
            .withRequestBody(WireMock.containing("master.caa%40gmail.com"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"access_token\":\"eyJzdWIiOiJjY2RfZ3ciLCJleHAiOjE1ODM0NDUyOTd9aa\"}")));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/o/userinfo"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"sub\":\"user@hmcts.net\",\"uid\":\"445\","
                    + "\"roles\":[\"caseworker-test\",\"pui-caa\"],\"name\":\"Cloud Strife\","
                    + "\"given_name\":\"Cloud\",\"family_name\":\"Strife\"}")));
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/refdata/external/v1/organisations/users"))
            .withQueryParam("status", WireMock.equalTo("Active"))
            .withQueryParam("returnRoles", WireMock.equalTo("false"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"users\":[],\"organisationIdentifier\":\"InvokingUsersOrg\"}")));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/refdata/internal/v1/organisations?id=Org2Id"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"contactInformation\":[{\"addressLine1\":\"Line 1\","
                    + "\"addressLine2\":\"Line 2\",\"addressLine3\":\"Line 3\","
                    + "\"county\":\"Kent\",\"townCity\":\"London\",\"country\":\"UK\","
                    + "\"postCode\":\"DA15 7LN\"}],\"organisationIdentifier\":\"Org2Id\","
                    + "\"name\":\"Org2Name\"}")));
    }
    
    private Jwt dummyJwt() {
        return Jwt.withTokenValue("a dummy jwt token")
                .claim("aClaim", "aClaim")
                .header("aHeader", "aHeader")
                .build();
    }

}
