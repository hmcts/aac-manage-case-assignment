#!/usr/bin/env bash

set -eu

dir=$(dirname ${0})

${dir}/utils/idam-create-service.sh "xui_webapp" "xui_webapp" "OOOOOOOOOOOOOOOO" "http://localhost:3333/oauth2/callback" "false" "profile openid roles manage-user create-user"