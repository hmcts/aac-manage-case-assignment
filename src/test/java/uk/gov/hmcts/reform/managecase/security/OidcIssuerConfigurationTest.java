package uk.gov.hmcts.reform.managecase.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class OidcIssuerConfigurationTest {

    private static final String PRIMARY_ISSUER = "https://idam-web-public.aat.platform.hmcts.net/o";
    private static final String SECONDARY_ISSUER =
        "https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/realms/root/realms/hmcts";
    private static final String TERTIARY_ISSUER = "https://issuer-to-enforce.example/openam/oauth2/hmcts";

    @Test
    void shouldFallbackToPrimaryIssuerWhenAllowedIssuersUnset() {
        assertThat(OidcIssuerConfiguration.allowedIssuers(PRIMARY_ISSUER, null))
            .containsExactly(PRIMARY_ISSUER);
    }

    @Test
    void shouldFallbackToPrimaryIssuerWhenAllowedIssuersBlank() {
        assertThat(OidcIssuerConfiguration.allowedIssuers(PRIMARY_ISSUER, " "))
            .containsExactly(PRIMARY_ISSUER);
    }

    @Test
    void shouldIncludePrimaryAndConfiguredAllowedIssuers() {
        assertThat(OidcIssuerConfiguration.allowedIssuers(
            PRIMARY_ISSUER,
            " " + SECONDARY_ISSUER + ", " + TERTIARY_ISSUER + " , " + SECONDARY_ISSUER + " "
        ))
            .containsExactly(PRIMARY_ISSUER, SECONDARY_ISSUER, TERTIARY_ISSUER);
    }

    @ParameterizedTest
    @MethodSource("allowedIssuerListsWithBlankEntries")
    void shouldIgnoreBlankConfiguredAllowedIssuerEntries(String configuredAllowedIssuers, String[] expectedIssuers) {
        assertThat(OidcIssuerConfiguration.allowedIssuers(PRIMARY_ISSUER, configuredAllowedIssuers))
            .containsExactly(expectedIssuers);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void shouldRejectBlankPrimaryIssuerEvenWhenAllowedIssuersAreConfigured(String primaryIssuer) {
        assertThatThrownBy(() -> OidcIssuerConfiguration.allowedIssuers(primaryIssuer, SECONDARY_ISSUER))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("oidc.issuer must not be blank");
    }

    private static Stream<Arguments> allowedIssuerListsWithBlankEntries() {
        return Stream.of(
            Arguments.of(", " + SECONDARY_ISSUER, new String[] {PRIMARY_ISSUER, SECONDARY_ISSUER}),
            Arguments.of(SECONDARY_ISSUER + ",", new String[] {PRIMARY_ISSUER, SECONDARY_ISSUER}),
            Arguments.of(
                SECONDARY_ISSUER + ",," + TERTIARY_ISSUER,
                new String[] {PRIMARY_ISSUER, SECONDARY_ISSUER, TERTIARY_ISSUER}
            )
        );
    }
}
