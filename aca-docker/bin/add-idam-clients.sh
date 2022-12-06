#!/usr/bin/env bash

set -eu

dir=$(dirname ${0})

${dir}/utils/idam-create-service.sh "xuiwebapp" "xuiwebapp" "OOOOOOOOOOOOOOOO" "http://localhost:3455/oauth2/callback" "false" "profile openid roles manage-user create-user search-user"
${dir}/utils/idam-create-service.sh "aac_manage_case_assignment" "aac_manage_case_assignment" "AAAAAAAAAAAAAAAA" "https://manage-case-assignment/oauth2redirect" "false" "profile openid roles manage-user"
