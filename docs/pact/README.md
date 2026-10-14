# Provider verification fixes

The provider tests run a real HTTP server with the application MVC gateway configuration. This covers servlet connection headers and `/ccd/searchCases`, which a standalone controller-only MockMvc target cannot exercise. CCD is stubbed at the downstream boundary; the provider's gateway and client-validation filters still execute.

The `Get list of cases` and `Handle caa case types` states now configure their dependencies. The verifier requests `Connection: close` so the servlet container supplies the corresponding transport response header.

## Required consumer correction

The `xui_manageOrg` contract at revision `1a4def9` also needs correction in `hmcts/rpx-xui-manage-organisations`:

- `case_ids=[123456789]` fails the provider's numeric ID validation. Send comma-separated numeric IDs without brackets.
- `case_title` is absent from this endpoint's response model.
- Use a populated case-role fixture, consistent with an existing assignment.

Apply `xui-manage-org-contract-fix.patch` from the consumer repository root:

```bash
git apply /path/to/aac-manage-case-assignment/docs/pact/xui-manage-org-contract-fix.patch
```

Regenerate and publish the corrected consumer Pact with a new consumer commit version and branch metadata, then rerun provider verification using the instructions in the repository README. The already-published `1a4def9` contract will continue failing on its invalid request and response expectations.

## Local regression checks

These checks do not require a Pact Broker:

```bash
./gradlew contract --tests '*CaseAssignmentProviderRegressionTest' checkstyleContractTest
```
