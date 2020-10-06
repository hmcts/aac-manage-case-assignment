#!/usr/bin/env bash

set -eu

dir=$(dirname ${0})

${dir}/utils/idam-add-role.sh "caseworker"
${dir}/utils/idam-add-role.sh "caseworker-caa"
${dir}/utils/idam-add-role.sh "caseworker-AUTOTEST1"
${dir}/utils/idam-add-role.sh "caseworker-AUTOTEST1-solicitor"

${dir}/utils/idam-add-role.sh "caseworker-BEFTA_MASTER"
${dir}/utils/idam-add-role.sh "caseworker-BEFTA_MASTER-solicitor"
${dir}/utils/idam-add-role.sh "caseworker-BEFTA_MASTER-solicitor_1"

