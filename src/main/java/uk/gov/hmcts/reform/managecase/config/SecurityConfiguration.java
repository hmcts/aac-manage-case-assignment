package uk.gov.hmcts.reform.managecase.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.managecase.security.JwtGrantedAuthoritiesConverter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);

    public static final String AUTHORISATION = "ServiceAuthorization";

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    @Value("${oidc.issuer}")
    private String issuerOverride;

    private final WebFilter serviceAuthFilter;
    private final Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthenticationConverter;

    private final List<String> authorisedServices;

    private final AuthTokenValidator authTokenValidator;

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
    public SecurityConfiguration(AuthTokenValidator authTokenValidator, List<String> authorisedServices,
                                 final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter) {
        super();
        this.authTokenValidator = authTokenValidator;
        if (authorisedServices == null || authorisedServices.isEmpty()) {
            throw new IllegalArgumentException("Must have at least one service defined");
        }
        this.authorisedServices = authorisedServices.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        this.serviceAuthFilter = serviceAuthWebFilter();
        jwtAuthenticationConverter = new Converter<Jwt, Mono<AbstractAuthenticationToken>>() {

            @Override
            public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
                Collection<GrantedAuthority> authorities = jwtGrantedAuthoritiesConverter.convert(jwt);
                String principalClaimValue = jwt.getClaimAsString(JwtClaimNames.SUB);
                return Mono.just(new JwtAuthenticationToken(jwt, authorities, principalClaimValue));
            }

        };
    }

    @Bean
    protected SecurityWebFilterChain filterChain(ServerHttpSecurity http) throws Exception {
        http
            .addFilterBefore(serviceAuthFilter, SecurityWebFiltersOrder.FIRST)
            .csrf(csrf -> csrf.disable())
            .formLogin(fl -> fl.disable())
            .logout(l -> l.disable())
            .authorizeExchange(ax -> ax.anyExchange().authenticated())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter)))
            .oauth2Client(null)
            ;
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(AUTH_ALLOWED_LIST);
    }

    protected WebFilter serviceAuthWebFilter() {
        return new WebFilter() {

            @Override
            public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();
                try {
                    String bearerToken = extractBearerToken(request);
                    String serviceName = authTokenValidator.getServiceName(bearerToken);
                    if (!authorisedServices.contains(serviceName)) {
                        log.debug("service forbidden {}", serviceName);
                        response.setStatusCode(HttpStatusCode.valueOf(HttpStatus.FORBIDDEN.value()));
                    } else {
                        log.debug("service authorized {}", serviceName);
                        return chain.filter(exchange);
                        //chain.doFilter(request, response);
                    }
                } catch (InvalidTokenException | ServiceException exception) {
                    log.warn("Unsuccessful service authentication", exception);
                    response.setStatusCode(HttpStatusCode.valueOf(HttpStatus.UNAUTHORIZED.value()));
                }
                return Mono.empty();
            }

            private String extractBearerToken(ServerHttpRequest request) {
                String token = request.getHeaders().getFirst(AUTHORISATION);
                if (token == null) {
                    throw new InvalidTokenException("ServiceAuthorization Token is missing");
                }
                return token.startsWith("Bearer ") ? token : "Bearer " + token;
            }

        };
    }

    @Bean
    @SuppressWarnings("PMD")
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromOidcIssuerLocation(issuerUri);
        // We are using issuerOverride instead of issuerUri as SIDAM has the wrong issuer at the moment
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(issuerOverride);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);
        jwtDecoder.setJwtValidator(validator);

        return jwtDecoder;
    }
}
