package uk.gov.hmcts.reform.managecase.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.managecase.security.JwtGrantedAuthoritiesConverter;

@Configuration
public class SecurityConfiguration {

    public static final String AUTHORISATION = "ServiceAuthorization";

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    @Value("${oidc.issuer}")
    private String issuerOverride;

    private final ServiceAuthFilter serviceAuthFilter;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    private static final String[] AUTH_ALLOWED_LIST = {
        "/swagger-resources/**",
        "/swagger-ui/**",
        "/webjars/**",
        "/v3/api-docs",
        "/health",
        "/health/liveness",
        "/health/readiness",
        "/info",
        "/favicon.ico",
        "/"
    };

    @Autowired
    public SecurityConfiguration(final ServiceAuthFilter serviceAuthFilter,
                                  final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter) {
        super();
        this.serviceAuthFilter = serviceAuthFilter;
        jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
    }

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(serviceAuthFilter, BearerTokenAuthenticationFilter.class)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .csrf(csrf -> csrf.disable()) // NOSONAR
            .formLogin(fl -> fl.disable())
            .logout(l -> l.disable())
            .authorizeHttpRequests(aht -> aht.anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter)))
            .oauth2Client(Customizer.withDefaults())
            ;
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(AUTH_ALLOWED_LIST);
    }

    @Bean
    @SuppressWarnings("PMD")
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromOidcIssuerLocation(issuerUri);
        // See docs/security/jwt-issuer-validation.md for issuer-uri discovery and oidc.issuer enforcement.
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> withIssuer = issuerValidator(issuerOverride);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);
        jwtDecoder.setJwtValidator(validator);

        return jwtDecoder;
    }

    static OAuth2TokenValidator<Jwt> issuerValidator(String issuerOverride) {
        List<String> allowedIssuers = allowedIssuers(issuerOverride);
        return jwt -> {
            String tokenIssuer = jwt.getClaimAsString(JwtClaimNames.ISS);
            if (tokenIssuer == null && jwt.getIssuer() != null) {
                tokenIssuer = jwt.getIssuer().toString();
            }

            if (allowedIssuers.contains(tokenIssuer)) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "The iss claim is not valid",
                null
            );
            return OAuth2TokenValidatorResult.failure(error);
        };
    }

    private static List<String> allowedIssuers(String issuerOverride) {
        return Arrays.stream(issuerOverride.split(","))
            .map(String::trim)
            .filter(issuer -> !issuer.isBlank())
            .toList();
    }
}
