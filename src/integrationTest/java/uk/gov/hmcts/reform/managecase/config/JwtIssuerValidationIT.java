package uk.gov.hmcts.reform.managecase.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.security.JwtGrantedAuthoritiesConverter;

class JwtIssuerValidationIT {

    private static final String DISCOVERY_ISSUER_PATH = "/o";
    private static final String ENFORCED_ISSUER = "https://issuer-to-enforce.example/openam/oauth2/hmcts";
    private static final String UNEXPECTED_ISSUER = "https://unexpected-issuer.example/openam/oauth2/hmcts";
    private static final String KEY_ID = "test-signing-key";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort())
        .build();

    private JwtDecoder jwtDecoder;
    private RSAPrivateKey privateKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();

        stubOidcDiscoveryAndJwks((RSAPublicKey) keyPair.getPublic());

        SecurityConfiguration configuration =
            new SecurityConfiguration(mock(), new JwtGrantedAuthoritiesConverter(mock(IdamRepository.class)));
        ReflectionTestUtils.setField(configuration, "issuerUri", wireMock.baseUrl() + DISCOVERY_ISSUER_PATH);
        ReflectionTestUtils.setField(configuration, "issuerOverride", ENFORCED_ISSUER);
        jwtDecoder = configuration.jwtDecoder();
    }

    @Test
    void shouldDecodeJwtWhenTokenIssMatchesConfiguredIssuer() throws Exception {
        assertDoesNotThrow(() -> jwtDecoder.decode(signedToken(ENFORCED_ISSUER)));
    }

    @Test
    void shouldRejectJwtWhenTokenIssDoesNotMatchConfiguredIssuer() throws Exception {
        String tokenWithUnexpectedIssuer = signedToken(UNEXPECTED_ISSUER);

        BadJwtException exception = assertThrows(
            BadJwtException.class,
            () -> jwtDecoder.decode(tokenWithUnexpectedIssuer)
        );

        assertThat(exception.getMessage()).contains("iss");
    }

    private void stubOidcDiscoveryAndJwks(RSAPublicKey publicKey) {
        String discoveryIssuer = wireMock.baseUrl() + DISCOVERY_ISSUER_PATH;
        String jwksUri = wireMock.baseUrl() + "/jwks";
        String jwksJson = new JWKSet(new RSAKey.Builder(publicKey)
            .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .keyID(KEY_ID)
            .build()).toString();

        wireMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(
            com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/o/.well-known/openid-configuration"))
            .willReturn(com.github.tomakehurst.wiremock.client.WireMock.okJson(
                "{\"issuer\":\"" + discoveryIssuer + "\",\"jwks_uri\":\"" + jwksUri + "\"}"
            )));

        wireMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(
            com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo("/jwks"))
            .willReturn(com.github.tomakehurst.wiremock.client.WireMock.okJson(jwksJson)));
    }

    private String signedToken(String issuer) throws JOSEException, ParseException {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("integration-user")
            .issueTime(Date.from(now.minusSeconds(60)))
            .expirationTime(Date.from(now.plusSeconds(300)))
            .build();

        SignedJWT signedJwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .keyID(KEY_ID)
                .build(),
            claims
        );
        signedJwt.sign(new RSASSASigner(privateKey));
        return signedJwt.serialize();
    }

    private KeyPair rsaKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }
}
