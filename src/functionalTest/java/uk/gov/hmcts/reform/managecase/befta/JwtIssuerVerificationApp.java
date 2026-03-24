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
import java.util.Base64;

public final class JwtIssuerVerificationApp {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JwtIssuerVerificationApp() {
    }

    public static void main(String[] args) {
        String expectedIssuer = requireEnv("OIDC_ISSUER");
        String actualIssuer = resolveIssuerFromRealToken();

        if (!expectedIssuer.equals(actualIssuer)) {
            throw new IllegalStateException(
                "OIDC_ISSUER mismatch: expected `" + expectedIssuer + "` but token iss was `" + actualIssuer + "`"
            );
        }

        System.out.println("Verified OIDC_ISSUER matches functional test token iss: " + actualIssuer);
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
        try {
            String authorisationCode = authorisationCode(
                httpClient,
                idamBaseUrl,
                username,
                password,
                clientId,
                redirectUri,
                scope
            );
            return accessToken(httpClient, idamBaseUrl, clientId, clientSecret, redirectUri, authorisationCode);
        } catch (IllegalStateException exception) {
            return passwordGrantAccessToken(
                httpClient,
                idamBaseUrl,
                username,
                password,
                clientId,
                clientSecret,
                redirectUri,
                scope,
                exception
            );
        }
    }

    private static String authorisationCode(HttpClient httpClient, String idamBaseUrl, String username, String password,
                                            String clientId, String redirectUri, String scope)
        throws IOException, InterruptedException {
        String authoriseUri = idamBaseUrl
            + "/oauth2/authorize?redirect_uri=" + encode(redirectUri)
            + "&response_type=code&client_id=" + encode(clientId)
            + "&scope=" + encode(scope);
        HttpRequest request = HttpRequest.newBuilder(URI.create(authoriseUri))
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .header("Authorization", basicAuth(username, password))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .build();

        JsonNode response = jsonResponse(
            httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
            "authorisation"
        );
        return requiredJsonText(response, "code", "authorisation response");
    }

    private static String accessToken(HttpClient httpClient, String idamBaseUrl, String clientId, String clientSecret,
                                      String redirectUri, String code)
        throws IOException, InterruptedException {
        String tokenUri = idamBaseUrl
            + "/oauth2/token?code=" + encode(code)
            + "&redirect_uri=" + encode(redirectUri)
            + "&grant_type=authorization_code";
        HttpRequest request = HttpRequest.newBuilder(URI.create(tokenUri))
            .POST(HttpRequest.BodyPublishers.ofString(""))
            .header("Authorization", basicAuth(clientId, clientSecret))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .build();

        JsonNode response = jsonResponse(httpClient.send(request, HttpResponse.BodyHandlers.ofString()), "token");
        return requiredJsonText(response, "access_token", "token response");
    }

    private static String passwordGrantAccessToken(HttpClient httpClient,
                                                   String idamBaseUrl,
                                                   String username,
                                                   String password,
                                                   String clientId,
                                                   String clientSecret,
                                                   String redirectUri,
                                                   String scope,
                                                   IllegalStateException authorisationFailure)
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

        try {
            JsonNode response = jsonResponse(
                httpClient.send(request, HttpResponse.BodyHandlers.ofString()),
                "password grant token"
            );
            return requiredJsonText(response, "access_token", "password grant token response");
        } catch (IllegalStateException passwordGrantFailure) {
            passwordGrantFailure.addSuppressed(authorisationFailure);
            throw passwordGrantFailure;
        }
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

    private static String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
