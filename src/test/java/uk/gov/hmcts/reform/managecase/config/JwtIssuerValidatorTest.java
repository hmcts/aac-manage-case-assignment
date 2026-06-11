package uk.gov.hmcts.reform.managecase.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
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

    private OAuth2TokenValidator<Jwt> validator() {
        return validator(null);
    }

    private OAuth2TokenValidator<Jwt> validator(String additionalAllowedIssuers) {
        return new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            SecurityConfiguration.issuerValidator(VALID_ISSUER, additionalAllowedIssuers)
        );
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
