#!/usr/bin/env bash

set -eu

email=${1}
rolesStr=${2}
surname=${3:-Test}
forename=${4:-User}
password=${5:-Pa55word11}
idamUrl=${IDAM_API_BASE_URL:-http://localhost:5000}

if [ -z "${rolesStr}" ]; then
  echo "Usage: ./idam-simulator-create-user.sh email roles [surname] [forename] [password]"
  exit 1
fi

IFS=',' read -ra roles <<< "${rolesStr}"

rolesJson="["
firstRole=true
for role in "${roles[@]}"; do
  if [ "$firstRole" = false ]; then
    rolesJson="${rolesJson},"
  fi
  rolesJson=${rolesJson}'{"code":"'${role}'"}'
  firstRole=false
done
rolesJson="${rolesJson}]"

echo "Creating IDAM simulator user: ${email}"

status=$(curl --insecure --show-error --silent --output /dev/null --write-out "%{http_code}" -X POST \
  "${idamUrl}/testing-support/accounts" \
  -H "Content-Type: application/json" \
  -d '{"email":"'${email}'","forename":"'${forename}'","surname":"'${surname}'","password":"'${password}'","roles":'${rolesJson}'}' || true)

if [ "$status" -eq 201 ] || [ "$status" -eq 200 ]; then
  echo "User ${email} - added to IDAM simulator"
elif [ "$status" -eq 403 ] || [ "$status" -eq 409 ]; then
  echo "User ${email} - already exists in IDAM simulator"
else
  echo "Unexpected HTTP status code from IDAM simulator: ${status}"
  exit 1
fi
