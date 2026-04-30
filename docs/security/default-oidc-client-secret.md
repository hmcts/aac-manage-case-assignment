# Default OIDC Client Secret Configuration

## Finding

`src/main/resources/application.yaml` defined an OAuth2 client registration for `oidc` with both the client id and client secret set to `internal`.

The service uses Spring Security as a resource server for bearer-token JWT validation. No production code was found that uses the configured OAuth2 client registration to obtain outbound tokens, so the default client secret was unused application configuration rather than a required runtime secret.

## Risk

Leaving a hardcoded default client secret in main application configuration creates avoidable security noise and could become a credential risk if future code starts using that client registration in a non-test environment.

## Remediation

The unused OAuth2 client registration was removed from application configuration. The unused OAuth2 client starter dependency, `oauth2Client` security configuration, and the test-only IDAM client registration shim were also removed.

JWT resource-server validation remains configured through `spring-boot-starter-oauth2-resource-server` and the existing `JwtDecoder`.

## Verification

Check that the default secret and OAuth2 client wiring are absent:

```bash
rg "client-secret: internal|spring-boot-starter-oauth2-client|oauth2Client|TestIdamConfiguration"
```

Compile the application and test sources:

```bash
./gradlew testClasses integrationTestClasses
```
