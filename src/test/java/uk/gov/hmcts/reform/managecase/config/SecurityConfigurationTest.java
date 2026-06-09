package uk.gov.hmcts.reform.managecase.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class SecurityConfigurationTest {

    private static final String VALID_ISSUER = "http://fr-am:8080/openam/oauth2/hmcts";
    private static final String CALLBACK_ISSUER =
        "https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/realms/root/realms/hmcts";
    private static final String INVALID_ISSUER = "http://unexpected-issuer";

    @Test
    void shouldAcceptJwtFromConfiguredIssuer() {
        assertFalse(validator().validate(buildJwt(VALID_ISSUER, Instant.now().plusSeconds(300))).hasErrors());
    }

    @Test
    void shouldAcceptJwtFromAdditionalConfiguredIssuer() {
        assertFalse(validator(CALLBACK_ISSUER)
            .validate(buildJwt(CALLBACK_ISSUER, Instant.now().plusSeconds(300)))
            .hasErrors());
    }

    @Test
    void shouldKeepPrimaryIssuerWhenAdditionalAllowedIssuersConfigured() {
        assertFalse(validator(CALLBACK_ISSUER)
            .validate(buildJwt(VALID_ISSUER, Instant.now().plusSeconds(300)))
            .hasErrors());
    }

    @Test
    void shouldRejectJwtFromUnexpectedIssuer() {
        assertTrue(validator().validate(buildJwt(INVALID_ISSUER, Instant.now().plusSeconds(300))).hasErrors());
    }

    @Test
    void shouldRejectJwtWithoutIssuer() {
        Instant now = Instant.now();
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("user")
            .issuedAt(now.minusSeconds(60))
            .expiresAt(now.plusSeconds(300))
            .build();

        assertTrue(validator().validate(jwt).hasErrors());
    }

    @Test
    void shouldRejectExpiredJwtEvenWhenIssuerMatches() {
        assertTrue(validator().validate(buildJwt(VALID_ISSUER, Instant.now().minusSeconds(180))).hasErrors());
    }

    @Test
    void shouldUseOidcIssuerAsPrimaryIssuer() throws IOException {
        MockEnvironment environment = applicationEnvironment(Map.of("OIDC_ISSUER", VALID_ISSUER));

        assertThat(environment.getProperty("oidc.issuer")).isEqualTo(VALID_ISSUER);
    }

    @Test
    void shouldLeaveAllowedIssuersEmptyWhenAllowedIssuersIsNotConfigured() throws IOException {
        MockEnvironment environment = applicationEnvironment(Map.of("OIDC_ISSUER", VALID_ISSUER));

        assertThat(environment.getProperty("oidc.allowed-issuers")).isEmpty();
    }

    @Test
    void shouldThrowDecoderExceptionContainingIssWhenJwtHasUnexpectedIssuer() throws Exception {
        KeyPair keyPair = JwtTestTokens.rsaKeyPair();
        JwtDecoder jwtDecoder = configuredJwtDecoder((RSAPublicKey) keyPair.getPublic());
        String tokenWithUnexpectedIssuer = JwtTestTokens.signedToken(
            INVALID_ISSUER,
            (RSAPrivateKey) keyPair.getPrivate()
        );
        BadJwtException exception = assertThrows(
            BadJwtException.class,
            () -> jwtDecoder.decode(tokenWithUnexpectedIssuer)
        );

        assertThat(exception.getMessage()).contains("iss");
    }

    private OAuth2TokenValidator<Jwt> validator() {
        return validator(null);
    }

    private OAuth2TokenValidator<Jwt> validator(String additionalAllowedIssuers) {
        return new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            SecurityConfiguration.issuerValidator(VALID_ISSUER, additionalAllowedIssuers)
        );
    }

    private JwtDecoder configuredJwtDecoder(RSAPublicKey publicKey) {
        NimbusJwtDecoder delegate = NimbusJwtDecoder.withPublicKey(publicKey).build();
        delegate.setJwtValidator(validator());
        return delegate;
    }

    private MockEnvironment applicationEnvironment(Map<String, String> properties) throws IOException {
        MockEnvironment environment = new MockEnvironment();
        properties.forEach(environment::setProperty);
        new YamlPropertySourceLoader()
            .load("applicationYaml", new ClassPathResource("application.yaml"))
            .forEach(propertySource -> environment.getPropertySources().addLast(propertySource));
        return environment;
    }

    private Jwt buildJwt(String issuer, Instant expiresAt) {
        Instant now = Instant.now();
        Instant issuedAt = expiresAt.isBefore(now.minusSeconds(60))
            ? expiresAt.minusSeconds(60)
            : now.minusSeconds(60);
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .issuer(issuer)
            .subject("user")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    }
}
