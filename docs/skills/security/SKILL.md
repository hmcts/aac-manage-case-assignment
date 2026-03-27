---
name: aac-manage-case-assignment-security
description: Use when working in the HMCTS `aac-manage-case-assignment` repository on general Spring Security, JWT and IDAM/OIDC integration, auth wiring, or related regression testing. For JWT issuer validation, use `docs/skills/security-jwt-issuer/SKILL.md`.
---

# Security

## Overview

Use this skill for general security changes in `aac-manage-case-assignment`.
For JWT issuer validation and issuer-wiring work, use [`docs/skills/security-jwt-issuer/SKILL.md`](../security-jwt-issuer/SKILL.md).

## Standard Workflow

1. Check current state with `git status --short` and inspect local diffs before editing.
2. Review `SecurityConfiguration` together with `application.yaml` before changing auth behavior.
3. Search for the touched security path before editing, for example `SecurityFilterChain`, `JwtDecoder`,
   `JwtIssuerValidator`, `ServiceAuthFilter`, `IDAM_OIDC_URL`, and `OIDC_ISSUER`.
4. Review the relevant runtime wiring and tests for the change you are making.
5. If the task turns into JWT issuer-validation work, switch to [`docs/skills/security-jwt-issuer/SKILL.md`](../security-jwt-issuer/SKILL.md).

## References

- JWT issuer-specific guidance: [`docs/skills/security-jwt-issuer/SKILL.md`](../security-jwt-issuer/SKILL.md)
