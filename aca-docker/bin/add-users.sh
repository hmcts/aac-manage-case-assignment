#!/usr/bin/env bash

set -eu

dir=$(dirname ${0})

echo "Using idam-sim at: ${IDAM_API_BASE_URL:-http://localhost:5000}"

jq -r '.[] | .email + " " + .roles + " " +  .lastName + " " + .firstName' ${dir}/users.json | while read args; do
  ${dir}/utils/idam-simulator-create-user.sh $args
done
