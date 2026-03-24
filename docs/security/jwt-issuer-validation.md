# JWT issuer validation

## Service

`aac-manage-case-assignment`

## Summary

- JWT issuer validation is enabled in the active `JwtDecoder`.
- OIDC discovery and issuer enforcement are configured separately on purpose.
- The enforced issuer must match the token `iss` claim.

## Discovery vs enforced issuer

- `spring.security.oauth2.client.provider.oidc.issuer-uri` is the discovery location. The service uses it to load OIDC metadata and the JWKS endpoint.
- `oidc.issuer` / `OIDC_ISSUER` is the enforced issuer value. The active `JwtDecoder` validates the token `iss` claim against this value.
- These values can differ. Discovery can point at the public IDAM OIDC endpoint while enforcement pins the exact `iss` emitted in accepted access tokens.

## Runtime behavior

- `SecurityConfiguration.jwtDecoder()` builds the decoder from `issuer-uri`.
- The decoder then applies both `JwtTimestampValidator` and `JwtIssuerValidator(oidc.issuer)`.
- Tokens signed by the discovered JWKS are still rejected if their `iss` does not exactly match `OIDC_ISSUER`.

## Implemented behavior

```java
OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(issuerOverride);
OAuth2TokenValidator<Jwt> validator =
    new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);
```

## Tests

`src/test/java/uk/gov/hmcts/reform/managecase/config/SecurityConfigurationTest.java` covers:

- accepted token from the configured issuer
- rejected token from an unexpected issuer
- rejected expired token from the configured issuer
- decoder exception for an unexpected issuer, asserting that the exception message contains `iss`

`src/integrationTest/java/uk/gov/hmcts/reform/managecase/config/JwtIssuerValidationIT.java` adds WireMock-backed
integration coverage for signed JWTs whose `iss` claim matches or does not match the configured issuer.

Coverage is intentionally three-layered in this repo:

- validator test
- decoder exception test
- integration test

Controller integration tests based on `src/integrationTest/java/uk/gov/hmcts/reform/managecase/BaseIT.java` mock
`JwtDecoder`, so they do not prove issuer validation.

## Assertion standard

For JWT issuer-validation tests and related verifier assertions in this repo:

- keep the existing repo-appropriate exception type for unexpected-issuer failures
- assert that the message contains `iss`
- do not assert the full exact framework message text
- use issuer terminology for config, docs, and comments
- use `iss` terminology for claim-level assertions and validator or decoder message checks

## Test and pipeline verification

- Focused tests cover valid issuer, invalid issuer, and expired token cases across validator, decoder, and integration layers.
- A single build-integrated verifier now acquires a real IDAM access token and compares its `iss` with `OIDC_ISSUER`.
- The verifier lives in `src/functionalTest/java/uk/gov/hmcts/reform/managecase/befta/JwtIssuerVerificationApp.java`.
- Gradle task `verifyFunctionalTestJwtIssuer` runs ahead of `smoke` and `functional` when `VERIFY_OIDC_ISSUER=true`.
- The verifier uses a real-token password-grant path with the existing BEFTA OIDC client settings and CAA test-user credentials.
- The verifier is a pre-check for real-token `iss` alignment. It is not the BEFTA functional-test auth implementation and does not need full BEFTA auth-path parity.
- Any mismatch text must stay in `iss` terms.

## Configuration and deployment note

Runtime configuration must still be correct:

- `spring.security.oauth2.client.provider.oidc.issuer-uri` is used for discovery and JWKS lookup
- `oidc.issuer` is the issuer value enforced during JWT validation
- in this repo those map to `IDAM_OIDC_URL` and `OIDC_ISSUER`

Check these files when issuer behavior changes:

- `src/main/resources/application.yaml`
- `charts/aac-manage-case-assignment/values.yaml`
- `charts/aac-manage-case-assignment/values.preview.template.yaml`
- `Jenkinsfile_CNP`
- `Jenkinsfile_parameterized`
- `aca-docker/bin/utils/lease-user-token.sh`
- `aca-docker/bin/utils/lease-service-token.sh`
- `aca-docker/bin/utils/idam-authenticate.sh`

Before rollout, confirm:

- each environment supplies the intended `OIDC_ISSUER`
- the `iss` claim in real caller tokens matches `OIDC_ISSUER`
- no Jenkins or release-time override is still supplying an older issuer value
- local and CI token acquisition paths still obtain tokens whose `iss` matches the configured enforced issuer

## How to derive `OIDC_ISSUER`

- Do not guess the issuer from the public discovery URL alone.
- Decode only the JWT payload from a real access token for the target environment and inspect the `iss` claim.
- Do not store or document full bearer tokens. Record only the derived issuer value.

Example:

```bash
TOKEN='eyJ...'
PAYLOAD=$(printf '%s' "$TOKEN" | cut -d '.' -f2)
python3 - <<'PY' "$PAYLOAD"
import base64, json, sys
payload = sys.argv[1]
payload += '=' * (-len(payload) % 4)
print(json.loads(base64.urlsafe_b64decode(payload))["iss"])
PY
```

- JWTs are `header.payload.signature`.
- The second segment is base64url-encoded JSON.
- This decodes the payload only. It does not verify the signature.

## Optional future variant

Only switch to multi-issuer validation if real environments genuinely require more than one accepted issuer during a
migration window. If that happens, use an explicit allow-list rather than dropping issuer validation.
