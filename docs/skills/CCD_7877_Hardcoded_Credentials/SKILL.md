# CCD-7877 Hardcoded Credentials

## Objective

Remove committed credential material from Manage Case Assignment and its local Docker configuration while retaining environment- and secret-store-based operation.

## Acceptance criteria

- No runtime password, username, client secret, S2S key, or plaintext secret file is committed.
- Local Docker obtains `MANAGE_CASE_S2S_KEY` and helper-script values from an untracked `.env` or the shell environment.
- Deployed environments continue to use the existing chart and Jenkins secret mappings.
- No live credential validation or rotation is claimed by this change.

## Findings and changes

- Removed credential defaults from `src/main/resources/application.yaml` and retained the existing variable names: `IDAM_CLIENT_SECRET`, `IDAM_CAA_USERNAME`, `IDAM_CAA_PASSWORD`, `IDAM_NOC_APPROVER_USERNAME`, `IDAM_NOC_APPROVER_PASSWORD`, and `MANAGE_CASE_S2S_KEY`.
- Removed the tracked `aca-docker/.env` and `aca-docker/bin/env_variables_all.txt` plaintext secret files. Use `aca-docker/bin/setup-local-secrets.sh` to create an ignored local `.env`, or use `aca-docker/.env.example` as a template.
- Local and AAT Compose definitions now require `MANAGE_CASE_S2S_KEY` to be supplied externally.
- AAC local database helpers now use the existing `IDAM_DB_PASSWORD`; they no longer assume a fixed `openidm` password. The AAC setup script does not generate or replace this value. When using the shared IDAM database provided by `ccd-docker`, the supplied value must match the value generated/configured by `ccd-docker`; AAC does not invoke or link to `ccd-docker`.
- Preview database values in `values.preview.template.yaml` still require pipeline-owner confirmation before replacement, because this checkout does not establish the required secret variable names.
- Existing Helm and Jenkins mappings are the deployment source of values; live values, use, and rotation status cannot be established from this checkout.

## Local validation

Run `./bin/setup-local-secrets.sh`, source the generated `.env`, and run the existing ACA Docker workflow. Compose should fail fast if `MANAGE_CASE_S2S_KEY` is absent and should interpolate it when present. For the shared-IDAM workflow and value ownership, follow the table in `aca-docker/README.md`. The generated values are disposable local values and are not replacements for AAT or production secrets.

## Recommendations

Use the service owners’ secret store for deployed values, rotate any historical values, and verify CI/CD variables, mounted secrets, running workloads, and rotation records before closure.
