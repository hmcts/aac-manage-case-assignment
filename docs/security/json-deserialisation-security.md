# JSON Deserialisation Security

This service must use strict Jackson deserialisation for AAC-owned inbound
request payloads.

Jackson must reject unknown JSON properties by default. This protects request,
callback, and domain DTOs from silently accepting unexpected fields.

Rejected HTTP requests return `400 BAD_REQUEST` and name the unknown property
path in the error message. The value of the rejected property is not included.

## Scope

Strict deserialisation is intended to validate JSON entering AAC through its
own API endpoints.

Downstream response DTOs are a separate compatibility concern. Provider
services such as CCD Data Store may add valid response fields over time. If AAC
uses strict deserialisation for those response models, any provider field that
is not explicitly modelled can cause AAC to fail while extracting the response
and return `502 BAD_GATEWAY`.

Example: CCD Data Store returns `version` on case details. If
`CaseDetails` does not model `version`, strict deserialisation rejects the
response even though the field is valid for the provider contract.

For downstream response DTOs, prefer one of the following:

- model known provider fields explicitly on the DTO
- if the provider contract is intentionally additive, use a deliberately scoped
  tolerant response DTO or client mapper with a documented rationale and test
  coverage

Do not disable strict deserialisation globally to handle downstream response
compatibility.

## Implementation

AAC now uses two deserialisation behaviours:

- **Inbound AAC HTTP requests:** Spring MVC uses the central strict
  `DefaultObjectMapper`. Unknown JSON properties are rejected and returned to
  the caller as `400 BAD_REQUEST`.
- **Downstream Feign responses:** provider-owned response bodies are decoded
  through a scoped tolerant decoder created by
  `DownstreamResponseDecoderFactory`. This decoder is wired only into the
  Data Store, Definition Store, and PRD Feign clients. It tolerates additive
  fields from those providers so AAC does not return `502 BAD_GATEWAY` only
  because a provider returned a new field.

This split is deliberate. It keeps AAC-owned input validation strict while
avoiding brittle coupling to additive response fields owned by other services.

### Spring Framework Response Compatibility

Spring MVC also serialises framework-owned error responses through the
configured HTTP message converters. Because AAC wires the strict
`DefaultObjectMapper` into those converters, the mapper must preserve Spring
Boot's existing `ProblemDetail` serialisation behaviour.

Register `ProblemDetailJacksonMixin` on the strict mapper. Without this mixin,
framework errors such as `GET /case-assignments` without the required
`case_ids` query parameter can include an unexpected `"properties": null`
field in the response body. That does not weaken request validation, but it
can break existing response-contract tests and callers that compare exact
error bodies.

This is an implementation compatibility requirement for the strict mapper. It
does not permit unknown JSON properties on AAC-owned request payloads.

Known provider metadata should still be modelled explicitly when it is part of
the contract AAC understands. For example, `CaseDetails` models CCD case
metadata such as `version`, callback status fields, and supplementary data.
The tolerant downstream decoder is a compatibility guard for future provider
additions, not a reason to leave known fields undocumented in code.

## Rules

Do not disable `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` globally.

The only allowed production-code exception is the scoped downstream response
decoder used for provider-owned Feign responses. Any additional exception must
be documented here and covered by tests.

Do not add:

- `@JsonIgnoreProperties(ignoreUnknown = true)`
- `@JsonAnySetter`
- custom unknown-property handlers such as `DeserializationProblemHandler`

If a caller adds a field this service needs, model that field explicitly on the
relevant AAC request DTO.

If a downstream service adds a field, decide whether the field is part of the
provider response contract that AAC should model, or whether the downstream
client should use a scoped tolerant response model. Document the decision in
the relevant test or architecture rule rationale.

For inbound request DTOs, prefer `@Getter` with constructors or builders over
Lombok `@Data`. Only add setters for fields that application code genuinely
mutates after deserialisation.

## Guardrails

The following tests protect this behaviour:

- `JacksonObjectMapperConfigTest`
- `JsonDeserialisationArchitectureTest`

`JacksonObjectMapperConfigTest` verifies that the configured `ObjectMapper`
rejects unknown properties and scans production source for known bypasses.
It allows the single downstream decoder exception and fails if other production
code disables unknown-property failures.

It also verifies that Spring `ProblemDetail` responses do not serialise a null
`properties` field when AAC uses the strict mapper for MVC message converters.

Controller and integration tests verify that MVC request deserialisation uses
the same strict behaviour and returns the rejected property name.

The `@S-207.6 @callbackTests` functional scenario asks CCD Data Store to fetch
the `ApplyNoCDecision` event trigger. That forces CCD to build its real
about-to-start callback payload and send it to ACA `/noc/apply-decision`,
guarding against missed CCD callback metadata fields such as
`case_details_before`, `event_id`, and `ignore_warning`.

`DownstreamResponseDecoderFactoryTest` verifies that provider-owned Feign
responses can contain additive fields without breaking downstream response
extraction.

`JsonDeserialisationArchitectureTest` uses ArchUnit to fail if
`@JsonIgnoreProperties(ignoreUnknown = true)` is reintroduced under
`src/main/java`.

Any exception must be added explicitly to `JsonDeserialisationArchitectureTest`
with a rationale explaining why accepting unknown properties is safe for that
type.

## Review Checklist

Before completing changes related to JSON deserialisation or DTOs, check:

- the rules above still hold for production code
- any required caller fields are modelled explicitly
- downstream response DTOs either model known provider metadata or have a
  documented scoped tolerance decision
- inbound request DTOs avoid broad setters unless mutation is required
- deserialisation behaviour changes are covered by tests

Run:

```bash
./gradlew test
./gradlew integration
./gradlew check
```

## Jira Risk Wording

Enabling strict Jackson deserialisation means `aac-manage-case-assignment` will
now reject unknown JSON fields on AAC-owned request and callback payloads. This
is the intended validation behaviour and may result in `400 BAD_REQUEST` if a
caller sends fields that are not part of the documented AAC contract.

There is also a downstream compatibility risk if the same strict behaviour is
applied to responses from provider services. A provider such as CCD Data Store
may return valid additive fields that AAC has not modelled locally. In that
case AAC can fail response extraction and return `502 BAD_GATEWAY`, as seen
when CCD Data Store returned the valid `version` field on `CaseDetails`.

The intended control is to keep strict validation for AAC inbound request DTOs,
while treating downstream response DTOs as provider contracts: model known
provider fields explicitly, or use a deliberately scoped tolerant response
model/client mapper with rationale and regression tests. Strict deserialisation
must not be disabled globally.

## Impact On Other Services

Services calling AAC may see `400 BAD_REQUEST` if they send unknown fields to
AAC-owned request or callback endpoints. This is intentional and should be
treated as contract validation.

Provider services called by AAC, including CCD Data Store, Definition Store,
and PRD, are not required to stop returning additive response fields. Their
Feign responses are decoded with a scoped tolerant mapper so additive provider
fields should not cause AAC to fail with `502 BAD_GATEWAY`.

This does not change the contracts of CCD Data Store, Definition Store, PRD,
Role Assignment, IDAM, or S2S. The change is internal to AAC deserialisation:
strict at AAC API boundaries, tolerant only at documented downstream response
boundaries.
