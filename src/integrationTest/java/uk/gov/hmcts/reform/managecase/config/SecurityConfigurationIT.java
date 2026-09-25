package uk.gov.hmcts.reform.managecase.config;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.Jwts;
import uk.gov.hmcts.reform.managecase.Application;
import uk.gov.hmcts.reform.managecase.utils.KeyGenUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = Application.class, properties = {
    "idam.security.allowed-issuers=http://localhost:${wiremock.server.port}/o,http://alternate.issuer/o"
})
@ActiveProfiles("integration")
@AutoConfigureWireMock(port = 0, stubs = "classpath:/wiremock-stubs")
class SecurityConfigurationIT {

    private static final String ISSUER = "http://localhost:%s/o";

    @Autowired
    private JwtDecoder jwtDecoder;

    @Value("${wiremock.server.port}")
    private Integer wiremockPort;

    @BeforeEach
    void setUp() throws Exception {
        RSAKey publicJwk = KeyGenUtil.getRsaJWK().toPublicJWK();
        String jwksJson = "{\"keys\":[" + publicJwk.toJSONString() + "]}";

        WireMock.reset();
        WireMock.stubFor(get(urlEqualTo("/jwks"))
            .willReturn(okJson(jwksJson)));
    }

    @Test
    void shouldDecodeJwtWhenIssuerAndTimestampAreValid() throws Exception {
        String issuer = issuer();
        String token = createToken(issuer, Instant.now().plusSeconds(300));

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getIssuer().toString()).isEqualTo(issuer);
    }

    @Test
    void shouldDecodeJwtWhenAlternateIssuerIsUsed() throws Exception {
        String issuer = "http://alternate.issuer/o";
        String token = createToken(issuer, Instant.now().plusSeconds(300));

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getIssuer().toString()).isEqualTo(issuer);
    }

    @Test
    void shouldRejectJwtWhenIssuerIsNotAllowed() throws Exception {
        String token = createToken("https://untrusted-issuer.example", Instant.now().plusSeconds(300));

        assertThatThrownBy(() -> jwtDecoder.decode(token))
            .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void shouldRejectJwtWhenTimestampHasExpired() throws Exception {
        String token = createToken(issuer(), Instant.now().minusSeconds(120));

        assertThatThrownBy(() -> jwtDecoder.decode(token))
            .isInstanceOf(JwtValidationException.class);
    }

    private String issuer() {
        return ISSUER.formatted(wiremockPort);
    }

    private String createToken(String issuer, Instant expiration) throws Exception {
        Instant issuedAt = expiration.minusSeconds(2);

        return Jwts.builder()
            .header().keyId(KeyGenUtil.getRsaJWK().getKeyID()).and()
            .issuer(issuer)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiration))
            .signWith(KeyGenUtil.getRsaJWK().toRSAPrivateKey(), Jwts.SIG.RS256)
            .compact();
    }

}