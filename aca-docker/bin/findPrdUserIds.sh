#!/usr/bin/env bash

which -s psql

if [ $? -ne 0 ]; then
  echo "Please install psql to run this script"
  exit 1;
fi

DB_USER_NAME=openidm
DB_PASSWORD=openidm
DB_HOST=localhost
DB_PORT=5432
DB_NAME=openidm

dir=$(dirname ${0})
cd "${dir}/../mocks/wiremock/__files"

JSON_USER_LIST=$(cat prd_users.json | jq -r '.users | map(.email) | @csv' | sed "s/\"/'/g")

SQL_COMMAND="SELECT openidm.managedobjects.fullobject ->> 'mail' as EMAIL, openidm.managedobjects.fullobject ->> '_id' as ID
FROM openidm.managedobjects
WHERE openidm.managedobjects.fullobject ->> 'mail' in ($JSON_USER_LIST) ORDER BY openidm.managedobjects.fullobject ->> 'mail' ASC;"

while IFS= read -r line
do
  email=$(echo $line | cut -d',' -f1)
  id=$(echo $line | cut -d',' -f2)
  echo "Email is $email, id is $id"

  case $email in
  'befta.master.solicitor.becky@gmail.com')
     sed -i '' "s/UID_BECKY/${id}/g" prd_users.json
    ;;
  'befta.master.solicitor.benjamin@gmail.com')
     sed -i '' "s/UID_BENJAMIN/${id}/g" prd_users.json
    ;;
  'befta.master.solicitor.bill@gmail.com')
     sed -i '' "s/UID_BILL/${id}/g" prd_users.json
    ;;
  'befta.master.solicitor.emma@gmail.com')
     sed -i '' "s/UID_EMMA/${id}/g" prd_users.json
    ;;
  'befta.pui.caa.1@gmail.com')
     sed -i '' "s/UID_PUI/${id}/g" prd_users.json
    ;;
  'befta.solicitor.4@gmail.com')
     sed -i '' "s/UID_SOLICITOR_4/${id}/g" prd_users.json
    ;;
  esac
done < <(psql "postgres://$DB_USER_NAME:$DB_PASSWORD@$DB_HOST:$DB_PORT/$DB_NAME" -t --csv -c "$SQL_COMMAND")

echo ""
echo -e "\e[1m\e[31m*** Values in prd_users.json have been updated ***"
echo -e "*** Please restart your aca-wiremock docker container for changes to take effect ***\e[0m"
