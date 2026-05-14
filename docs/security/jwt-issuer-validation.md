# JWT issuer validation

## Service

`aac-manage-case-assignment`

## Summary

- JWT issuer validation is enabled in the active `JwtDecoder`.
- OIDC discovery and issuer enforcement are configured separately on purpose.
- The token `iss` claim must match one of the explicitly configured enforced issuers.
- See [HMCTS Guidance](#hmcts-guidance) for the central policy reference.

<a id="hmcts-guidance"></a>
## HMCTS Guidance

- HMCTS guidance: [JWT iss Claim Validation guidance](https://tools.hmcts.net/confluence/spaces/SISM/pages/1958056812/JWT+iss+Claim+Validation+for+OIDC+and+OAuth+2+Tokens#JWTissClaimValidationforOIDCandOAuth2Tokens-Configurationrecommendation)
- Use that guidance as the reference point for service-level issuer decisions and configuration recommendations.

## Quick Reference

| Topic | Current repo position |
| --- | --- |
| Validation model | Explicit comma-separated issuer allow-list |
| Discovery source | `spring.security.oauth2.client.provider.oidc.issuer-uri` |
| Enforced issuer | `oidc.issuer` / `OIDC_ISSUER` |
| Current service wiring | Helm values, preview values, and `Jenkinsfile_CNP` explicitly set `OIDC_ISSUER` to public IDAM `/o` plus ForgeRock realm issuer values |
| Nightly wiring | `Jenkinsfile_nightly` uses the same AAT public plus ForgeRock issuer allow-list as `Jenkinsfile_CNP` |
| Runtime fallback | Main runtime config enforces `oidc.issuer` from `OIDC_ISSUER`, with a static local fallback, and does not derive it from discovery |

### Guidance alignment

| Item | Current repo state |
| --- | --- |
| Service issuer model | Explicit issuer allow-list |
| Issuer pattern used for this service | Repo config currently accepts public IDAM `/o` and ForgeRock realm issuer values |
| Repo wiring status | Helm values, preview values, `Jenkinsfile_CNP`, and `Jenkinsfile_nightly` are aligned to that allow-list pattern |
| Nightly status | `Jenkinsfile_nightly` is aligned with the AAT allow-list pattern |
| External alignment check | AAT has been verified from a real token as `https://idam-web-public.aat.platform.hmcts.net/o`; confirm other target environments before rollout |

## Discovery vs enforced issuer

- `spring.security.oauth2.client.provider.oidc.issuer-uri` is the discovery location used to load OIDC metadata and the JWKS endpoint.
- `oidc.issuer` / `OIDC_ISSUER` is the comma-separated issuer allow-list matched against the token `iss` claim.
- These values can differ. Discovery can point at the public IDAM OIDC endpoint while enforcement pins the exact `iss` values emitted in accepted access tokens.

## Runtime behavior

- `SecurityConfiguration.jwtDecoder()` builds the decoder from `issuer-uri`.
- The decoder then applies both `JwtTimestampValidator` and the explicit issuer allow-list from `oidc.issuer`.
- Tokens signed by the discovered JWKS are still rejected if their `iss` does not match any configured `OIDC_ISSUER` allow-list entry.

## Implemented behavior

```java
OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
OAuth2TokenValidator<Jwt> withIssuer = issuerValidator(issuerOverride);
OAuth2TokenValidator<Jwt> validator =
    new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);
```

## Tests

`src/test/java/uk/gov/hmcts/reform/managecase/config/SecurityConfigurationTest.java` covers:

- accepted token from the configured issuer
- accepted token from the second configured issuer
- rejected token from an unexpected issuer
- rejected expired token from the configured issuer
- decoder exception for an unexpected issuer, asserting that the exception message contains `iss`

`src/integrationTest/java/uk/gov/hmcts/reform/managecase/config/JwtIssuerValidationIT.java` adds WireMock-backed
integration coverage for signed JWTs whose `iss` claim matches one allow-list entry, a second allow-list entry, or no
allow-list entry.

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

## CCD submitted callbacks

NoC auto-approval submits a CCD event that triggers a CCD submitted callback back into this service at
`/noc/check-noc-approval`.

That flow has two separate token surfaces:

- `CaseEventCreationPayload.on_behalf_of_token` is part of the CCD event payload and must contain the raw access
  token from `SecurityUtils.getUserToken()`, without a `Bearer ` prefix.
- The CCD submitted callback back into this service is an HTTP request. Its `Authorization` header must be a bearer
  header, `Bearer <jwt>`, and that JWT `iss` claim must match one of the configured `OIDC_ISSUER` values.

These are different failure modes even though both can surface as `401 Unauthorized`.

- If `on_behalf_of_token` includes a `Bearer ` prefix, CCD later calls IDAM `/o/userinfo` with a malformed token and
  NoC submission can fail with `502 Bad Gateway` and an `invalid_token` log in data-store.
- If the CCD submitted callback request reaches `/noc/check-noc-approval` but the callback HTTP `Authorization` header
  is missing, malformed, or has the wrong `iss`, NoC submission can fail with `Submitted callback failed` and a callback
  `401 Unauthorized` log in data-store.

### PR-711 issuer mismatch diagnosis

On 14 May 2026, PR-711 NoC callback failures were diagnosed by enabling CCD Data Store callback request logging with:

```text
LOG_CALLBACK_DETAILS=noc/check-noc-approval,noc/apply-decision
```

The failing CCD submitted callback to `/noc/check-noc-approval` contained both:

- `ServiceAuthorization` for `ccd_data`
- `Authorization: Bearer <jwt>`

The bearer token `iss` was:

```text
https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/realms/root/realms/hmcts
```

The MCA preview pod had been enforcing only:

```text
https://idam-web-public.aat.platform.hmcts.net/o
```

That mismatch caused MCA to reject the CCD callback with `401 Unauthorized`, which Data Store surfaced as
`Submitted callback failed`. Manual curl confirmed that MCA accepted a token from the public issuer and rejected
S2S-only callback requests, so the failure was specifically an issuer mismatch on the real CCD callback bearer token.

During the IDAM migration window, this service accepts an explicit allow-list containing both public IDAM and ForgeRock
realm issuer values. Remove the ForgeRock issuer only after all real callers, including CCD submitted callbacks, emit
public issuer tokens.

## Test and pipeline verification

- Focused tests cover valid issuer, invalid issuer, and expired token cases across validator, decoder, and integration layers.
- A single build-integrated verifier now acquires a real IDAM access token and confirms its `iss` appears in the
  `OIDC_ISSUER` allow-list.
- The verifier lives in `src/functionalTest/java/uk/gov/hmcts/reform/managecase/befta/JwtIssuerVerificationApp.java`.
- Gradle task `verifyFunctionalTestJwtIssuer` runs ahead of `smoke` and `functional` when `VERIFY_OIDC_ISSUER=true`.
- The verifier uses a real-token password-grant path with the existing BEFTA OIDC client settings and CAA test-user credentials.
- The verifier is a pre-check for real-token `iss` alignment. It is not the BEFTA functional-test auth implementation and does not need full BEFTA auth-path parity.
- Any mismatch text must stay in `iss` terms.
- `DataStoreRepositoryTest` covers the NoC CCD event payload and should assert that `on_behalf_of_token` is the raw
  token from `SecurityUtils.getUserToken()`.

## Configuration and deployment note

Runtime configuration must still be correct:

| Config area | Meaning in this repo |
| --- | --- |
| `IDAM_OIDC_URL` | Discovery base used for OIDC metadata and JWKS lookup |
| `OIDC_ISSUER` | Comma-separated enforced issuer allow-list matched against the token `iss` claim |
| Change rule | If `OIDC_ISSUER` changes, update Helm, preview, and Jenkins together |

Service-level issuer decisions should be checked against [HMCTS Guidance](#hmcts-guidance).

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
- the `iss` claim in real caller tokens matches one configured `OIDC_ISSUER` entry
- no Jenkins or release-time override is still supplying an older issuer value
- local and CI token acquisition paths still obtain tokens whose `iss` matches one configured issuer entry
- NoC CCD event payloads use a raw access token in `on_behalf_of_token`, not a bearer header value
- CCD submitted callback HTTP requests use a valid bearer `Authorization` header whose `iss` matches one configured
  `OIDC_ISSUER` entry

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

This service uses multi-issuer validation because real AAT callback traffic currently emits both public IDAM and
ForgeRock realm issuer values during the migration window. The allow-list is explicit and must stay narrow; do not
replace issuer validation with permissive validation.

If issuer strategy changes are proposed for this service, confirm them against the HMCTS configuration recommendation:
[HMCTS Guidance](#hmcts-guidance)

## Acceptance Checklist

Before merging JWT issuer-validation changes, confirm all of the following:

- The active `JwtDecoder` is built from `spring.security.oauth2.client.provider.oidc.issuer-uri`.
- The active validator chain includes both `JwtTimestampValidator` and explicit issuer allow-list validation.
- There is no disabled, commented-out, or alternate runtime path that leaves issuer validation off.
- `issuer-uri` is used for discovery and JWKS lookup only.
- `oidc.issuer` / `OIDC_ISSUER` is used as the enforced token `iss` allow-list only.
- `OIDC_ISSUER` is explicitly configured and not guessed from the discovery URL.
- App config, Helm values, preview values, and any Jenkins files that explicitly set `OIDC_ISSUER` are aligned for the target environment.
- If `OIDC_ISSUER` changed, it was verified against a real token for the target environment.
- NoC CCD event payloads pass a raw access token, not a bearer header value, in `on_behalf_of_token`.
- CCD submitted callback HTTP requests use a bearer `Authorization` header whose token `iss` matches one configured
  `OIDC_ISSUER` entry.
- There is a test that accepts a token with an expected issuer entry.
- There is a test that rejects a token with an unexpected issuer.
- There is a test that rejects an expired token.
- There is decoder-level coverage using a signed token, not only validator-only coverage.
- At least one failure assertion clearly proves issuer rejection, for example by checking for `iss`.
- When `VERIFY_OIDC_ISSUER=true`, CI or build verification checks that a real token issuer appears in `OIDC_ISSUER`,
  or the repo documents why that does not apply.
- Comments and docs do not describe the old insecure behavior.
- Any repo-specific difference from peer services is intentional and documented.

Do not merge if any of the following are true:

- issuer validation is constructed but not applied
- only timestamp validation is active
- `OIDC_ISSUER` was inferred rather than verified
- Helm and CI/Jenkins issuer values disagree without explanation
- `on_behalf_of_token` is populated with a `Bearer `-prefixed value
- CCD submitted callback HTTP requests use a raw JWT, missing token, or token with an `iss` outside the allow-list
- only happy-path tests exist

## Configuration Policy

- `spring.security.oauth2.client.provider.oidc.issuer-uri` is used for OIDC discovery and JWKS lookup only.
- `oidc.issuer` / `OIDC_ISSUER` is the enforced JWT issuer allow-list and the token `iss` claim must match one entry.
- Do not derive `OIDC_ISSUER` from `IDAM_OIDC_URL` or the discovery URL.
- Production-like environments must provide `OIDC_ISSUER` explicitly.
- Requiring explicit `OIDC_ISSUER` with no static fallback in main runtime config is the preferred pattern, but it is not yet mandatory across all services.
- Local or test-only fallbacks are acceptable only when they are static, intentional, and clearly scoped to non-production use.
- The build enforces this policy with `verifyOidcIssuerPolicy`, which fails if `oidc.issuer` is derived from discovery config.

## References

- [HMCTS Guidance](#hmcts-guidance)
