package uk.gov.hmcts.reform.managecase.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import uk.gov.hmcts.reform.managecase.repository.IdamRepository;
import uk.gov.hmcts.reform.managecase.security.JwtGrantedAuthoritiesConverter;

class JwtIssuerValidationIT {

    private static final String DISCOVERY_ISSUER_PATH = "/o";
    private static final String ENFORCED_ISSUER = "https://issuer-to-enforce.example/openam/oauth2/hmcts";
    private static final String CALLBACK_ISSUER =
        "https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/realms/root/realms/hmcts";
    private static final String UNEXPECTED_ISSUER = "https://unexpected-issuer.example/openam/oauth2/hmcts";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort())
        .build();

    private JwtDecoder jwtDecoder;
    private RSAPrivateKey privateKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPair keyPair = JwtTestTokens.rsaKeyPair();
        privateKey = (RSAPrivateKey) keyPair.getPrivate();

        stubOidcDiscoveryAndJwks((RSAPublicKey) keyPair.getPublic());

        SecurityConfiguration configuration = new SecurityConfiguration(
            wireMock.baseUrl() + DISCOVERY_ISSUER_PATH,
            ENFORCED_ISSUER,
            CALLBACK_ISSUER,
            mock(),
            new JwtGrantedAuthoritiesConverter(mock(IdamRepository.class))
        );
        jwtDecoder = configuration.jwtDecoder();
    }

    @Test
    void shouldDecodeJwtWhenTokenIssMatchesConfiguredIssuer() throws Exception {
        assertDoesNotThrow(() -> jwtDecoder.decode(JwtTestTokens.signedToken(ENFORCED_ISSUER, privateKey)));
    }

    @Test
    void shouldDecodeJwtWhenTokenIssMatchesAdditionalConfiguredIssuer() throws Exception {
        assertDoesNotThrow(() -> jwtDecoder.decode(JwtTestTokens.signedToken(CALLBACK_ISSUER, privateKey)));
    }

    @Test
    void shouldRejectJwtWhenTokenIssDoesNotMatchConfiguredIssuer() throws Exception {
        String tokenWithUnexpectedIssuer = JwtTestTokens.signedToken(UNEXPECTED_ISSUER, privateKey);

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
            .keyID(JwtTestTokens.KEY_ID)
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
}
