#!/usr/bin/env bash

set -euo pipefail

script_dir=$(cd "$(dirname "$0")" && pwd)
env_file="${script_dir}/../.env"

if [ -e "$env_file" ]; then
  echo "Refusing to overwrite existing $env_file" >&2
  exit 1
fi

command -v openssl >/dev/null 2>&1 || {
  echo "openssl is required to generate local values" >&2
  exit 1
}

if [ -z "${IDAM_DB_PASSWORD:-}" ]; then
  echo "IDAM_DB_PASSWORD must be set before running this script" >&2
  echo "Set it from the approved local IDAM database configuration, then retry" >&2
  exit 1
fi

umask 077

local_test_user_password=$(openssl rand -hex 16)

cat > "$env_file" <<EOF
# Generated for local AAC Docker use. Do not commit or use for AAT/production.
APPINSIGHTS_INSTRUMENTATIONKEY=local-only
WIREMOCK_SERVER_MAPPINGS_PATH=wiremock
SERVER_PORT=4454
MANAGE_CASE_S2S_KEY=$(openssl rand -hex 8)
IDAM_CLIENT_SECRET=$(openssl rand -hex 16)
XUIWEBAPP_IDAM_CLIENT_SECRET=$(openssl rand -hex 16)
CCD_API_GATEWAY_IDAM_CLIENT_SECRET=$(openssl rand -hex 16)
NOTIFY_MCA_API_KEY=$(openssl rand -hex 16)
IDAM_TEST_USER_PASSWORD=${local_test_user_password}
IDAM_DB_PASSWORD=${IDAM_DB_PASSWORD}
IDAM_CAA_USERNAME=master.caa@gmail.com
IDAM_CAA_PASSWORD=${local_test_user_password}
IDAM_NOC_APPROVER_USERNAME=noc.approver@gmail.com
IDAM_NOC_APPROVER_PASSWORD=${local_test_user_password}
EOF

chmod 600 "$env_file"
echo "Created $env_file with disposable local values. Source it before running AAC scripts."
