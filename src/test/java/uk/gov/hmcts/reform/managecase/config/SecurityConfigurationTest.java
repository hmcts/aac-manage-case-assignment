package uk.gov.hmcts.reform.managecase.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import java.time.Instant;

class SecurityConfigurationTest {

    private static final String VALID_ISSUER = "http://fr-am:8080/openam/oauth2/hmcts";
    private static final String INVALID_ISSUER = "http://unexpected-issuer";
    private static final String KEY_ID = "unit-test-signing-key";

    @Test
    void shouldAcceptJwtFromConfiguredIssuer() {
        assertFalse(validator().validate(buildJwt(VALID_ISSUER, Instant.now().plusSeconds(300))).hasErrors());
    }

    @Test
    void shouldRejectJwtFromUnexpectedIssuer() {
        assertTrue(validator().validate(buildJwt(INVALID_ISSUER, Instant.now().plusSeconds(300))).hasErrors());
    }

    @Test
    void shouldRejectExpiredJwtEvenWhenIssuerMatches() {
        assertTrue(validator().validate(buildJwt(VALID_ISSUER, Instant.now().minusSeconds(180))).hasErrors());
    }

    @Test
    void shouldThrowDecoderExceptionContainingIssWhenJwtHasUnexpectedIssuer() throws Exception {
        KeyPair keyPair = rsaKeyPair();
        JwtDecoder jwtDecoder = configuredJwtDecoder((RSAPublicKey) keyPair.getPublic());
        String tokenWithUnexpectedIssuer = signedToken(INVALID_ISSUER, (RSAPrivateKey) keyPair.getPrivate());
        BadJwtException exception = assertThrows(
            BadJwtException.class,
            () -> jwtDecoder.decode(tokenWithUnexpectedIssuer)
        );

        assertThat(exception.getMessage()).contains("iss");
    }

    private OAuth2TokenValidator<Jwt> validator() {
        return new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            new JwtIssuerValidator(VALID_ISSUER)
        );
    }

    private JwtDecoder configuredJwtDecoder(RSAPublicKey publicKey) {
        NimbusJwtDecoder delegate = NimbusJwtDecoder.withPublicKey(publicKey).build();
        delegate.setJwtValidator(validator());
        return delegate;
    }

    private String signedToken(String issuer, RSAPrivateKey privateKey) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject("unit-test-user")
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
