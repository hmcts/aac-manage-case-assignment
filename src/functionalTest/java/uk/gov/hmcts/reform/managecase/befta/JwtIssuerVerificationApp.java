package uk.gov.hmcts.reform.managecase.befta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.managecase.security.OidcIssuerConfiguration;

@Slf4j
public final class JwtIssuerVerificationApp {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JwtIssuerVerificationApp() {
    }

    public static void main(String[] args) {
        Set<String> allowedIssuers = configuredIssuers();
        String actualIssuer = resolveIssuerFromRealToken();

        if (!allowedIssuers.contains(actualIssuer)) {
            throw new IllegalStateException(
                "OIDC allowed issuer mismatch: expected one of `" + allowedIssuers + "` but token iss was `"
                    + actualIssuer + "`"
            );
        }

        log.info("Verified functional test token iss is allowed by OIDC issuer configuration: {}", actualIssuer);
    }

    private static Set<String> configuredIssuers() {
        return OidcIssuerConfiguration.allowedIssuers(
            requireEnv("OIDC_ISSUER"),
            optionalEnv("OIDC_ALLOWED_ISSUERS", "")
        );
    }

    private static String resolveIssuerFromRealToken() {
        try {
            String accessToken = acquireAccessToken();
            String issuer = SignedJWT.parse(accessToken).getJWTClaimsSet().getIssuer();
            if (issuer == null || issuer.isBlank()) {
                throw new IllegalStateException("Decoded IDAM access token did not contain an iss claim");
            }
            return issuer;
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to acquire IDAM access token for issuer verification", exception);
        } catch (ParseException exception) {
            throw new IllegalStateException("Failed to parse IDAM access token as a JWT", exception);
        }
    }

    private static String acquireAccessToken() throws IOException, InterruptedException {
        String idamBaseUrl = requireEnv("IDAM_API_URL_BASE");
        String username = requireEnv("BEFTA_IDAM_CAA_USERNAME");
        String password = requireEnv("BEFTA_IDAM_CAA_PASSWORD");
        String clientId = requireEnv("BEFTA_OAUTH2_CLIENT_ID_OF_XUIWEBAPP");
        String clientSecret = requireEnv("BEFTA_OAUTH2_CLIENT_SECRET_OF_XUIWEBAPP");
        String redirectUri = requireEnv("BEFTA_OAUTH2_REDIRECT_URI_OF_XUIWEBAPP");
        String scope = requireEnv("BEFTA_OAUTH2_SCOPE_VARIABLES_OF_XUIWEBAPP");

        HttpClient httpClient = HttpClient.newHttpClient();
        return passwordGrantAccessToken(
            httpClient,
            idamBaseUrl,
            username,
            password,
            clientId,
            clientSecret,
            redirectUri,
            scope
        );
    }

    private static String passwordGrantAccessToken(HttpClient httpClient,
                                                   String idamBaseUrl,
                                                   String username,
                                                   String password,
                                                   String clientId,
                                                   String clientSecret,
                                                   String redirectUri,
                                                   String scope)
        throws IOException, InterruptedException {
        String tokenUri = idamBaseUrl + "/o/token";
        String formBody = "client_id=" + encode(clientId)
            + "&client_secret=" + encode(clientSecret)
            + "&grant_type=password"
            + "&redirect_uri=" + encode(redirectUri)
            + "&username=" + encode(username)
            + "&password=" + encode(password)
            + "&scope=" + encode(scope);
        HttpRequest request = HttpRequest.newBuilder(URI.create(tokenUri))
            .POST(HttpRequest.BodyPublishers.ofString(formBody))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .build();

        JsonNode response = jsonResponse(
            httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
            "password grant token"
        );
        return requiredJsonText(response, "access_token", "password grant token response");
    }

    private static JsonNode jsonResponse(HttpResponse<String> response, String callName) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("IDAM " + callName + " call failed with status "
                                                + response.statusCode() + ": " + response.body());
        }
        return OBJECT_MAPPER.readTree(response.body());
    }

    private static String requiredJsonText(JsonNode jsonNode, String fieldName, String responseName) {
        JsonNode field = jsonNode.get(fieldName);
        if (field == null || field.asText().isBlank()) {
            throw new IllegalStateException("IDAM " + responseName + " did not contain `" + fieldName + "`");
        }
        return field.asText();
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable `" + name + "`");
        }
        return value;
    }

    private static String optionalEnv(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
