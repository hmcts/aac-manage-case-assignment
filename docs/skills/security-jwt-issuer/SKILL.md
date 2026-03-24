---
name: aac-manage-case-assignment-security-jwt-issuer
description: Use when working in the HMCTS `aac-manage-case-assignment` repository on JWT issuer validation, OIDC discovery versus enforced issuer configuration, Helm or Jenkins OIDC_ISSUER settings, build-integrated issuer verification, or related regression testing.
---

# Security JWT Issuer

## Overview

Use this skill for JWT issuer validation changes in `aac-manage-case-assignment`.

## Workflow

1. Check current diffs with `git status --short` before editing.
2. Review [`src/main/java/uk/gov/hmcts/reform/managecase/config/SecurityConfiguration.java`](../../../src/main/java/uk/gov/hmcts/reform/managecase/config/SecurityConfiguration.java), [`src/main/resources/application.yaml`](../../../src/main/resources/application.yaml), [`charts/aac-manage-case-assignment/values.yaml`](../../../charts/aac-manage-case-assignment/values.yaml), [`Jenkinsfile_CNP`](../../../Jenkinsfile_CNP), and [`Jenkinsfile_parameterized`](../../../Jenkinsfile_parameterized).
3. Confirm the split between discovery and enforcement:
   `spring.security.oauth2.client.provider.oidc.issuer-uri` is for discovery and JWKS.
   `oidc.issuer` / `OIDC_ISSUER` is the enforced issuer matched against the token `iss` claim.
4. Search for `issuer`, `issuer-uri`, `JwtDecoder`, `JwtIssuerValidator`, `JwtTimestampValidator`, `OIDC_ISSUER`, and `VERIFY_OIDC_ISSUER` before changing behavior.
5. Preserve the single build-integrated verifier pattern:
   use [`src/functionalTest/java/uk/gov/hmcts/reform/managecase/befta/JwtIssuerVerificationApp.java`](../../../src/functionalTest/java/uk/gov/hmcts/reform/managecase/befta/JwtIssuerVerificationApp.java)
   and [`build.gradle`](../../../build.gradle) task `verifyFunctionalTestJwtIssuer`
   rather than duplicate smoke or functional verifier tests or Jenkins-side issuer-resolution scripts.
6. Keep coverage focused across three layers:
   validator-level tests in [`src/test/java/uk/gov/hmcts/reform/managecase/config/SecurityConfigurationTest.java`](../../../src/test/java/uk/gov/hmcts/reform/managecase/config/SecurityConfigurationTest.java),
   decoder exception assertions in the same test class,
   and decoder integration coverage in [`src/integrationTest/java/uk/gov/hmcts/reform/managecase/config/JwtIssuerValidationIT.java`](../../../src/integrationTest/java/uk/gov/hmcts/reform/managecase/config/JwtIssuerValidationIT.java).
7. For issuer values, token `iss` diagnosis, test assertions, and rollout alignment, follow [`docs/security/jwt-issuer-validation.md`](../../../docs/security/jwt-issuer-validation.md) rather than duplicating that guidance here.
8. Start verification with the narrowest useful test:
   - `./gradlew test --tests uk.gov.hmcts.reform.managecase.config.SecurityConfigurationTest`
9. Preserve in-flight local work and continue from the existing patch state rather than recreating it.

## References

- Primary repo guidance: [`docs/security/jwt-issuer-validation.md`](../../../docs/security/jwt-issuer-validation.md)
- Security workflow: [`docs/skills/security/SKILL.md`](../security/SKILL.md)
