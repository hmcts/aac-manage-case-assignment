#!/usr/bin/env bash

which -s psql

if [ $? -ne 0 ]; then
  echo "Please install psql to run this script"
  exit 1;
fi

# console colours / fonts
RED=$(tput setaf 1)
BOLD=$(tput bold)
NORMAL=$(tput sgr0)

DB_USER_NAME=openidm
DB_PASSWORD=openidm
DB_HOST=localhost
DB_PORT=5432
DB_NAME=openidm

dir=$(dirname ${0})
cd "${dir}/../mocks/wiremock/__files"

function generatePrdUsersFileFromTemplate() {
  JSON_USER_LIST=$(cat "$1" | jq -r '.users | map(.email) | @csv' | sed "s/\"/'/g")

  SQL_COMMAND="SELECT openidm.managedobjects.fullobject ->> 'mail' as EMAIL, openidm.managedobjects.fullobject ->> '_id' as ID
  FROM openidm.managedobjects
  WHERE openidm.managedobjects.fullobject ->> 'mail' in ($JSON_USER_LIST) ORDER BY openidm.managedobjects.fullobject ->> 'mail' ASC;"

  JSON_TEMPLATE_FILE=$(<"$1")

  OUTPUT_FILE='{ "users" : ['
  while IFS= read -r line
  do
    email=$(echo $line | cut -d',' -f1)
    id=$(echo $line | cut -d',' -f2)
    echo "Email is $email, id is $id"
    newJsonWithId=$(echo $JSON_TEMPLATE_FILE | jq '.users[] | select(.email=="'${email}'").userIdentifier="'${id}'" | select(.userIdentifier=="'${id}'")')

    # append the Json object with the updated id and add a comma
    OUTPUT_FILE+="$newJsonWithId,"
  done < <(psql "postgres://$DB_USER_NAME:$DB_PASSWORD@$DB_HOST:$DB_PORT/$DB_NAME" -t --csv -c "$SQL_COMMAND")

  # remove the last character from the file - the trailing comma
  OUTPUT_FILE=${OUTPUT_FILE%?}

  # find the organisationIdentifier value from the template
  ORG_ID=$(echo $JSON_TEMPLATE_FILE | jq '.organisationIdentifier')

  # add organistationIdfentifier to json
  OUTPUT_FILE+='], "organisationIdentifier": '${ORG_ID}'}'
  echo $OUTPUT_FILE | jq . > "$2"
  echo "${RED}${BOLD}*** Values in $2 have been updated ***${NORMAL}"
  echo ""
}

generatePrdUsersFileFromTemplate "prd_users_template.json" "prd_users.json"
generatePrdUsersFileFromTemplate "prd_users_template_Org-Two.json" "prd_users_Org-Two.json"

echo -e "${RED}${BOLD}*** Please restart your aca-wiremock docker container for changes to take effect ***${NORMAL}"
