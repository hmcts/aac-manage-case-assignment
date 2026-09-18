# JWT issuer validation

## Service

`aac-manage-case-assignment`

## Summary

- JWT issuer validation is enabled in the active `JwtDecoder`.
- OIDC discovery and issuer enforcement are configured separately on purpose.
- The token `iss` claim must match one explicitly configured issuer.
- See [HMCTS Guidance](#hmcts-guidance) for the central policy reference.

<a id="hmcts-guidance"></a>
## HMCTS Guidance

- HMCTS guidance: [JWT iss Claim Validation guidance](https://tools.hmcts.net/confluence/spaces/SISM/pages/1958056812/JWT+iss+Claim+Validation+for+OIDC+and+OAuth+2+Tokens#JWTissClaimValidationforOIDCandOAuth2Tokens-Configurationrecommendation)
- Use that guidance as the reference point for service-level issuer decisions and configuration recommendations.

## Quick Reference

| Topic | Current repo position |
| --- | --- |
| Validation model | Primary issuer plus optional additional allowed-issuer list |
| Discovery source | `spring.security.oauth2.client.provider.oidc.issuer-uri` |
| Primary issuer | `oidc.issuer` / `OIDC_ISSUER`, one exact issuer value |
| Allowed issuers | `oidc.allowed-issuers` / `OIDC_ALLOWED_ISSUERS`, optional comma-separated additional exact issuer values |
| Current service wiring | Preview and `Jenkinsfile_CNP` keep `OIDC_ISSUER` as public IDAM `/o` and set `OIDC_ALLOWED_ISSUERS` to include the ForgeRock issuer used by CCD submitted callbacks |
| Nightly wiring | `Jenkinsfile_nightly` still sets the ForgeRock AAT2 realm issuer and must be reviewed before relying on nightly issuer verification |
| Runtime fallback | Main runtime config defaults `oidc.allowed-issuers` to empty; `OIDC_ISSUER` is always included by code and is not derived from discovery |

### Guidance alignment

| Item | Current repo state |
| --- | --- |
| Service issuer model | Primary issuer plus optional additional allowed-issuer list |
| Issuer pattern used for this service | Public IDAM `/o` is still the primary issuer; preview also accepts ForgeRock callback tokens from CCD |
| Repo wiring status | Preview values and `Jenkinsfile_CNP` are aligned to the AAT callback evidence; non-preview Helm values remain single public issuer until proven otherwise |
| Nightly status | `Jenkinsfile_nightly` is not aligned with the public issuer pattern |
| External alignment check | AAT has been verified from a real token as `https://idam-web-public.aat.platform.hmcts.net/o`; confirm other target environments before rollout |

## Discovery vs enforced issuer

- `spring.security.oauth2.client.provider.oidc.issuer-uri` is the discovery location used to load OIDC metadata and the JWKS endpoint.
- `oidc.issuer` / `OIDC_ISSUER` is the primary issuer value.
- `oidc.allowed-issuers` / `OIDC_ALLOWED_ISSUERS` adds optional extra issuer values matched against the token
  `iss` claim.
- `OIDC_ALLOWED_ISSUERS` may contain one additional issuer or a comma-separated list of additional exact issuer values.
- When `OIDC_ALLOWED_ISSUERS` is not set, only `OIDC_ISSUER` is accepted.
- These values can differ. Discovery can point at the public IDAM OIDC endpoint while enforcement pins the exact `iss` emitted in accepted access tokens.

## Runtime behavior

- `SecurityConfiguration.jwtDecoder()` builds the decoder from `issuer-uri`.
- The decoder then applies both `JwtTimestampValidator` and an exact issuer allow-list built from `oidc.issuer`
  plus optional entries from `oidc.allowed-issuers`.
- Tokens signed by the discovered JWKS are still rejected if their `iss` does not exactly match one value from
  `OIDC_ISSUER` or `OIDC_ALLOWED_ISSUERS`.

## Implemented behavior

```java
OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
OAuth2TokenValidator<Jwt> withIssuer = issuerValidator(issuer, allowedIssuers);
OAuth2TokenValidator<Jwt> validator =
    new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);
```

## Tests

`src/test/java/uk/gov/hmcts/reform/managecase/config/JwtIssuerValidatorTest.java` covers:

- accepted token from the configured issuer
- accepted token from an additional configured issuer
- rejected token from an unexpected issuer
- rejected token with no issuer
- rejected expired token from the configured issuer
- primary issuer remains accepted when additional issuers are configured

`src/test/java/uk/gov/hmcts/reform/managecase/security/OidcIssuerConfigurationTest.java` covers fallback to the
primary issuer, blank additional issuer config, duplicate additional issuer config, and empty issuer rejection.

`src/integrationTest/java/uk/gov/hmcts/reform/managecase/config/JwtDecoderIssuerValidationIT.java` adds WireMock-backed
integration coverage for `SecurityConfiguration.jwtDecoder()` with signed JWTs whose `iss` claim matches either
configured issuer or does not match any configured issuer. It asserts the real decoder rejection message contains
`iss`.

Coverage is intentionally split in this repo:

- fast validator unit test
- real decoder integration test

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
  header, `Bearer <jwt>`, and that JWT `iss` claim must exactly match one configured allowed issuer value.

These are different failure modes even though both can surface as `401 Unauthorized`.

- If `on_behalf_of_token` includes a `Bearer ` prefix, CCD later calls IDAM `/o/userinfo` with a malformed token and
  NoC submission can fail with `502 Bad Gateway` and an `invalid_token` log in data-store.
- If the CCD submitted callback request reaches `/noc/check-noc-approval` but the callback HTTP `Authorization` header
  is missing, malformed, or has the wrong `iss`, NoC submission can fail with `Submitted callback failed` and a callback
  `401 Unauthorized` log in data-store.

### Preview CCD Data Store dependency

MCA preview currently accepts:

```text
OIDC_ISSUER=https://idam-web-public.aat.platform.hmcts.net/o
OIDC_ALLOWED_ISSUERS=https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/realms/root/realms/hmcts
```

> **Key point:** CCD Data Store calls MCA for NoC submitted callbacks. Those callbacks must include a bearer token whose
> `iss` claim matches one of MCA's configured issuers. Valid S2S alone is not enough.

We proved that pointing BEFTA at CCD Data Store PR-2830 is not enough: Data Store still has single-issuer validation,
and the PR verifier reaches Data Store with a public IDAM token while Data Store still expects the CCD gateway callback
issuer. Making Data Store public-only would reject existing CCD gateway and callback tokens.

The callback-compatible fix is therefore in MCA: accept the primary public IDAM issuer and the exact ForgeRock issuer
observed on CCD submitted callback bearer tokens during the IDAM migration window. This does not disable issuer
validation; it keeps exact `iss` matching against a short allow-list.

## Test and pipeline verification

- Focused tests cover valid issuer, invalid issuer, and expired token cases across validator, decoder, and integration layers.
- A single build-integrated verifier now acquires a real IDAM access token and checks its `iss` is allowed by
  `OIDC_ISSUER` plus optional entries from `OIDC_ALLOWED_ISSUERS`.
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
| `OIDC_ISSUER` | Primary exact issuer value |
| `OIDC_ALLOWED_ISSUERS` | Optional comma-separated additional exact issuer values matched against the token `iss` claim |
| Change rule | If issuer config changes, update Helm, preview, and Jenkins together |

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

- each environment supplies the intended `OIDC_ISSUER` and optional `OIDC_ALLOWED_ISSUERS`
- the `iss` claim in real caller tokens matches one configured issuer
- no Jenkins or release-time override is still supplying an older issuer value
- local and CI token acquisition paths still obtain tokens whose `iss` matches the configured enforced issuer
- NoC CCD event payloads use a raw access token in `on_behalf_of_token`, not a bearer header value
- CCD submitted callback HTTP requests use a valid bearer `Authorization` header whose `iss` matches one configured issuer

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

## Allow-list scope

The allow-list exists because the service currently receives two legitimate token issuers on different paths:

- public IDAM `/o` tokens from BEFTA and normal user-facing calls
- ForgeRock AAT2 tokens on CCD submitted callback bearer headers

Do not add broad domains, discovery URLs, or wildcard-style issuer values. Add only exact `iss` values observed from
real accepted caller tokens, and keep the list under review as clients migrate.

If issuer strategy changes are proposed for this service, confirm them against the HMCTS configuration recommendation:
[HMCTS Guidance](#hmcts-guidance)

## Acceptance Checklist

Before merging JWT issuer-validation changes, confirm all of the following:

- The active `JwtDecoder` is built from `spring.security.oauth2.client.provider.oidc.issuer-uri`.
- The active validator chain includes both `JwtTimestampValidator` and exact issuer allow-list validation from
  `oidc.issuer` plus optional entries from `oidc.allowed-issuers`.
- There is no disabled, commented-out, or alternate runtime path that leaves issuer validation off.
- `issuer-uri` is used for discovery and JWKS lookup only.
- `oidc.issuer` / `OIDC_ISSUER` is used as the primary issuer only.
- `oidc.allowed-issuers` / `OIDC_ALLOWED_ISSUERS` is used only for additional accepted token `iss` values.
- `OIDC_ISSUER` is explicitly configured and not guessed from the discovery URL.
- App config, Helm values, preview values, and any Jenkins files that explicitly set issuer config are aligned for the target environment.
- If issuer config changed, it was verified against real tokens for the target environment.
- NoC CCD event payloads pass a raw access token, not a bearer header value, in `on_behalf_of_token`.
- CCD submitted callback HTTP requests use a bearer `Authorization` header whose token `iss` matches one configured issuer.
- There is a test that accepts a token with the expected issuer.
- There is a test that rejects a token with an unexpected issuer.
- There is a test that rejects an expired token.
- There is decoder-level coverage using a signed token, not only validator-only coverage.
- At least one failure assertion clearly proves issuer rejection, for example by checking for `iss`.
- When `VERIFY_OIDC_ISSUER=true`, CI or build verification checks that a real token issuer is allowed by issuer config, or the repo documents why that does not apply.
- Comments and docs do not describe the old insecure behavior.
- Any repo-specific difference from peer services is intentional and documented.

Do not merge if any of the following are true:

- issuer validation is constructed but not applied
- only timestamp validation is active
- `OIDC_ISSUER` or `OIDC_ALLOWED_ISSUERS` was inferred rather than verified
- Helm and CI/Jenkins issuer values disagree without explanation
- `on_behalf_of_token` is populated with a `Bearer `-prefixed value
- CCD submitted callback HTTP requests use a raw JWT, missing token, or token with the wrong `iss`
- only happy-path tests exist

## Configuration Policy

- `spring.security.oauth2.client.provider.oidc.issuer-uri` is used for OIDC discovery and JWKS lookup only.
- `oidc.issuer` / `OIDC_ISSUER` is the primary JWT issuer.
- `oidc.allowed-issuers` / `OIDC_ALLOWED_ISSUERS` contains optional additional JWT issuers, and the token
  `iss` claim must exactly match `OIDC_ISSUER` or one configured additional issuer.
- Do not derive `OIDC_ISSUER` from `IDAM_OIDC_URL` or the discovery URL.
- Production-like environments must provide `OIDC_ISSUER` explicitly.
- Requiring explicit `OIDC_ISSUER` with no static fallback in main runtime config is the preferred pattern, but it is not yet mandatory across all services.
- Local or test-only fallbacks are acceptable only when they are static, intentional, and clearly scoped to non-production use.
- The build enforces this policy with `verifyOidcIssuerPolicy`, which fails if `oidc.issuer` is derived from discovery config.

## References

- [HMCTS Guidance](#hmcts-guidance)
