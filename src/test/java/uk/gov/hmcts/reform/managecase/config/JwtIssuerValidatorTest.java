package uk.gov.hmcts.reform.managecase.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

class JwtIssuerValidatorTest {

    private static final String VALID_ISSUER = "http://fr-am:8080/openam/oauth2/hmcts";
    private static final String CALLBACK_ISSUER =
        "https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/realms/root/realms/hmcts";
    private static final String INVALID_ISSUER = "http://unexpected-issuer";
    private static final Instant NOW = Instant.parse("2024-01-01T12:00:00Z");
    private static final Instant VALID_EXPIRY = NOW.plusSeconds(300);
    private static final Instant EXPIRED_AT = NOW.minusSeconds(180);

    @Test
    void shouldAcceptJwtFromConfiguredIssuer() {
        assertFalse(validator().validate(buildJwt(VALID_ISSUER, VALID_EXPIRY)).hasErrors());
    }

    @Test
    void shouldAcceptJwtFromAdditionalConfiguredIssuer() {
        assertFalse(validator(CALLBACK_ISSUER)
            .validate(buildJwt(CALLBACK_ISSUER, VALID_EXPIRY))
            .hasErrors());
    }

    @Test
    void shouldKeepPrimaryIssuerWhenAdditionalAllowedIssuersConfigured() {
        assertFalse(validator(CALLBACK_ISSUER)
            .validate(buildJwt(VALID_ISSUER, VALID_EXPIRY))
            .hasErrors());
    }

    @Test
    void shouldRejectJwtFromUnexpectedIssuer() {
        assertTrue(validator().validate(buildJwt(INVALID_ISSUER, VALID_EXPIRY)).hasErrors());
    }

    @Test
    void shouldRejectJwtWithoutIssuer() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("user")
            .issuedAt(NOW.minusSeconds(60))
            .expiresAt(VALID_EXPIRY)
            .build();

        assertTrue(validator().validate(jwt).hasErrors());
    }

    @Test
    void shouldRejectExpiredJwtEvenWhenIssuerMatches() {
        assertTrue(validator().validate(buildJwt(VALID_ISSUER, EXPIRED_AT)).hasErrors());
    }

    private OAuth2TokenValidator<Jwt> validator() {
        return validator(null);
    }

    private OAuth2TokenValidator<Jwt> validator(String additionalAllowedIssuers) {
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator();
        timestampValidator.setClock(Clock.fixed(NOW, ZoneOffset.UTC));
        return new DelegatingOAuth2TokenValidator<>(
            timestampValidator,
            SecurityConfiguration.issuerValidator(VALID_ISSUER, additionalAllowedIssuers)
        );
    }

    private Jwt buildJwt(String issuer, Instant expiresAt) {
        Instant issuedAt = expiresAt.isBefore(NOW.minusSeconds(60))
            ? expiresAt.minusSeconds(60)
            : NOW.minusSeconds(60);
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .issuer(issuer)
            .subject("user")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .build();
    }
}
