# ISSUE-094 Task: Organisation Boundary Validation for `POST /case-users`

## Summary
`aac-manage-case-assignment` accepts `organisation_id` from request payload and currently only validates that it is non-empty when present. This allows a caller to submit another organisation's ID.

The service must treat caller organisation as server-side authority and validate any request `organisation_id` against it.

## Scope
Endpoint:
- `POST /case-users`

Primary code area:
- `src/main/java/uk/gov/hmcts/reform/managecase/api/controller/CaseAssignedUserRolesController.java`

Supporting code likely needed:
- PRD-backed current organisation lookup, reusing existing `PrdRepository.findUsersByOrganisation()`

## Required Behaviour
- Resolve the caller's organisation using `PrdRepository.findUsersByOrganisation()`.
- Do not trust `organisation_id` from the request as authority.
- If `organisation_id` is present and blank, reject the request.
- If `organisation_id` is present and does not match the caller's organisation, reject the request.
- If product rules require organisation context for this endpoint, reject missing `organisation_id` too.
- Reuse a single helper/service for caller-org lookup rather than duplicating PRD access in the controller.

## Expected Response
Decision required:
- Prefer `400 Bad Request` if treated as invalid request data.
- Prefer `403 Forbidden` if treated as an authorization failure.

Choose one contract and keep tests aligned.

## Acceptance Criteria
- A solicitor cannot submit `organisation_id` for another organisation.
- A solicitor can submit their own organisation ID.
- Blank `organisation_id` is rejected.
- Existing valid same-org flows continue to work.

## Tests
Added:
- Controller/unit test for mismatched organisation
- Controller/unit test for matching organisation
- Controller/unit test for blank organisation
- Controller/unit test for missing organisation
- Integration test for matching organisation
- Integration test for mismatched organisation
- Integration test for missing organisation
- Integration test for caller organisation not present on case organisation policies
- Functional test for mismatched organisation
- Functional test for matching organisation
- Functional test for missing organisation
- Functional test for caller organisation not present on case organisation policies

## Non-goals
- Do not redesign PRD integration.
- Do not change unrelated case assignment behaviour.
 and
