# CCD-7227 Agent

## Repo State

- Repo: `aac-manage-case-assignment`
- Branch: `CCD-CCD-7227_Default_OIDC_Client_Secret_Configuration`
- Base: `origin/master`
- Worktree: `/Users/ilapatel/Documents/Work/repos/.worktrees/CCD-7227/aac-manage-case-assignment`
- Status when this note was created: changes are intentionally uncommitted.

## Purpose

Remove the hardcoded default OIDC OAuth2 client secret from main application configuration.

The key finding is that this service validates inbound bearer JWTs as a resource server. No production code was found that uses the configured OAuth2 client registration to obtain outbound tokens, so the `client-secret: internal` value was unused configuration and could be removed with the unused OAuth2 client wiring.

The service remains configured as an OAuth2 resource server for inbound caller bearer JWT validation. Existing S2S validation/authorisation wiring and outbound S2S token generation are unchanged.

## Changes Made

- Removed `spring.security.oauth2.client.registration.oidc.client-id` and `client-secret` from `src/main/resources/application.yaml`.
- Removed `spring-boot-starter-oauth2-client` from `build.gradle`.
- Removed `.oauth2Client(...)` from `src/main/java/uk/gov/hmcts/reform/managecase/config/SecurityConfiguration.java`.
- Removed `TestIdamConfiguration` from `src/integrationTest/java/uk/gov/hmcts/reform/managecase/BaseIT.java`.
- Removed `TestIdamConfiguration` imports and `@ImportAutoConfiguration` use from controller tests.
- Deleted `src/test/java/uk/gov/hmcts/reform/managecase/TestIdamConfiguration.java`.
- Added `src/test/java/uk/gov/hmcts/reform/managecase/config/SecurityConfigurationTest.java` to prove bearer JWT resource-server wiring still works without OAuth2 client registration.
- Added `docs/security/default-oidc-client-secret.md`.
- Added `docs/skills/default-oidc-client-secret/SKILL.md`.

## Resume Guidance

Do not reintroduce OAuth2 client registration unless new code genuinely needs to acquire tokens as an OAuth2 client. Keep resource-server JWT validation separate from OAuth2 client concerns.

Useful checks:

```bash
rg -n "client-secret: internal|spring-boot-starter-oauth2-client|\.oauth2Client\(|TestIdamConfiguration|ClientRegistrationRepository|OAuth2AuthorizedClient" --glob '!docs/**'
git diff --check
./gradlew test --tests uk.gov.hmcts.reform.managecase.config.SecurityConfigurationTest --offline --no-daemon
./gradlew testClasses integrationTestClasses --offline --no-daemon
```

## Verification Already Run

- Code grep above returned no matches outside `docs`.
- `git diff --check` passed.
- `./gradlew test --tests uk.gov.hmcts.reform.managecase.config.SecurityConfigurationTest --offline --no-daemon` passed.
- `./gradlew testClasses integrationTestClasses --offline --no-daemon` passed.

The non-offline Gradle run compiled main sources but failed while resolving the test classpath because Maven/Azure artifact metadata requests hit an SSL/PKIX certificate validation issue.
