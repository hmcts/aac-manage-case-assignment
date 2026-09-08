#!/usr/bin/env bash

set -euo pipefail

# console colours / fonts
if [ -t 1 ]; then
  RED=$(tput setaf 1 || true)
  BOLD=$(tput bold || true)
  NORMAL=$(tput sgr0 || true)
else
  RED=""
  BOLD=""
  NORMAL=""
fi

DB_USER_NAME=${DB_USER_NAME:-openidm}
DB_PASSWORD=${DB_PASSWORD:-openidm}
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-openidm}
IDAM_API_BASE_URL=${IDAM_API_BASE_URL:-http://localhost:5000}

dir=$(dirname "${0}")
cd "${dir}/../mocks/wiremock/__files"

function requireCommand() {
  if ! command -v "$1" > /dev/null 2>&1; then
    echo "Please install $1 to run this script"
    exit 1
  fi
}

function lookupIdamSimulatorUserId() {
  local email=$1
  local responseFile
  local status
  local userId

  responseFile=$(mktemp)
  status=$(curl --insecure --show-error --silent --get \
    --output "${responseFile}" \
    --write-out "%{http_code}" \
    "${IDAM_API_BASE_URL}/testing-support/accounts" \
    --data-urlencode "email=${email}" || true)

  if [ "${status}" != "200" ]; then
    rm -f "${responseFile}"
    return 1
  fi

  userId=$(jq -r '.id // empty' "${responseFile}")
  rm -f "${responseFile}"

  if [ -z "${userId}" ]; then
    return 1
  fi

  echo "${userId}"
}

function idamSimulatorAccountEndpointAvailable() {
  local responseFile
  local status

  responseFile=$(mktemp)
  status=$(curl --insecure --show-error --silent \
    --output "${responseFile}" \
    --write-out "%{http_code}" \
    "${IDAM_API_BASE_URL}/testing-support/accounts" || true)
  rm -f "${responseFile}"

  [ "${status}" = "400" ] || [ "${status}" = "200" ]
}

function writePrdUsersFile() {
  local templateFile=$1
  local outputFile=$2
  local lookupCommand=$3
  local tempFile
  local email
  local id

  tempFile=$(mktemp "${outputFile}.XXXXXX")
  cp "${templateFile}" "${tempFile}"

  while IFS= read -r email
  do
    id=$(${lookupCommand} "${email}" || true)

    if [ -z "${id}" ]; then
      rm -f "${tempFile}"
      echo "${RED}${BOLD}Could not resolve IDAM user id for ${email}.${NORMAL}"
      echo "Run the relevant IDAM/idam-sim user setup first, then rerun this script."
      exit 1
    fi

    echo "Email is ${email}, id is ${id}"
    jq --arg email "${email}" --arg id "${id}" \
      '(.users[] | select(.email == $email) | .userIdentifier) = $id' \
      "${tempFile}" > "${tempFile}.updated"
    mv "${tempFile}.updated" "${tempFile}"
  done < <(jq -r '.users[].email' "${templateFile}")

  if ! jq -e 'all(.users[]; (.userIdentifier // "") != "")' "${tempFile}" > /dev/null; then
    rm -f "${tempFile}"
    echo "${RED}${BOLD}Generated PRD users file still contains empty userIdentifier values.${NORMAL}"
    exit 1
  fi

  mv "${tempFile}" "${outputFile}"
  echo "${RED}${BOLD}*** Values in ${outputFile} have been updated ***${NORMAL}"
  echo ""
}

function lookupPostgresUserId() {
  local email=$1

  psql "postgres://${DB_USER_NAME}:${DB_PASSWORD}@${DB_HOST}:${DB_PORT}/${DB_NAME}" -t --csv -c \
    "SELECT openidm.managedobjects.fullobject ->> '_id' as ID
    FROM openidm.managedobjects
    WHERE openidm.managedobjects.fullobject ->> 'mail' = '${email}'
    LIMIT 1;" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//'
}

function generatePrdUsersFileFromPostgres() {
  requireCommand psql
  writePrdUsersFile "$1" "$2" lookupPostgresUserId
}

function generatePrdUsersFileFromIdamSimulator() {
  writePrdUsersFile "$1" "$2" lookupIdamSimulatorUserId
}

function restartAcaWiremockIfRunning() {
  local containers

  if ! command -v docker > /dev/null 2>&1; then
    echo "${RED}${BOLD}*** Please restart your aca-wiremock docker container for changes to take effect ***${NORMAL}"
    return
  fi

  if ! containers=$(docker ps --format '{{.Names}}' 2> /dev/null); then
    echo "${RED}${BOLD}*** Please restart your aca-wiremock docker container for changes to take effect ***${NORMAL}"
    return
  fi

  if printf '%s\n' "${containers}" | grep -qx "aca-wiremock"; then
    echo "Restarting aca-wiremock to reload generated PRD user data..."
    docker restart aca-wiremock > /dev/null
    echo "${RED}${BOLD}*** aca-wiremock restarted ***${NORMAL}"
  else
    echo "${RED}${BOLD}*** Please start or restart your aca-wiremock docker container for changes to take effect ***${NORMAL}"
  fi
}

requireCommand jq
requireCommand curl

# The template files contain the BEFTA users defined in the actors list below
# https://tools.hmcts.net/confluence/display/RCCD/MCA+BEFTA+Actors+list

if idamSimulatorAccountEndpointAvailable; then
  echo "Using idam-sim at: ${IDAM_API_BASE_URL}"
  generatePrdUsersFileFromIdamSimulator "prd_users_template_organisation_01.json" "prd_users_organisation_01.json"
  generatePrdUsersFileFromIdamSimulator "prd_users_template_organisation_02.json" "prd_users_organisation_02.json"
else
  echo "idam-sim account lookup is not available at ${IDAM_API_BASE_URL}; using full IDAM Postgres."
  generatePrdUsersFileFromPostgres "prd_users_template_organisation_01.json" "prd_users_organisation_01.json"
  generatePrdUsersFileFromPostgres "prd_users_template_organisation_02.json" "prd_users_organisation_02.json"
fi

restartAcaWiremockIfRunning
