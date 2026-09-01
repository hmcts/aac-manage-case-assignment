# ACA fullstack docker :whale:

- [Prerequisites](#prerequisites)
- [ACA Scripts](#ACA)
- [License](#license)

## Prerequisites

- [Docker](https://www.docker.com)
- psql (Tested with version 12.4)
- `ccd-docker` environment configured and running,
see [Run `ccd-docker` containers](#Run-ccd-docker-containers) for details

### Environment Variables
- Create the ignored local environment file from the example and set the required local values:

    ```bash
    cd aca-docker
    ./bin/setup-local-secrets.sh
    set -a
    source .env
    set +a
  ```

  The script refuses to overwrite an existing `.env`. Do not commit `.env`; deployed values must continue to come from the service secret store.

  AAC helper scripts that query the local IDAM database require the existing `IDAM_DB_PASSWORD` environment variable. Set it from the approved local IDAM database configuration before running this script; AAC does not generate or replace the value. When using the shared IDAM database provided by `ccd-docker`, this must be the matching value generated/configured by `ccd-docker`. AAC does not invoke or link to `ccd-docker`.

| Value | Generated or supplied by | AAC requirement |
|---|---|---|
| `IDAM_DB_PASSWORD` | The setup that owns the local IDAM database, normally `ccd-docker` | Supply the matching value; AAC does not generate or replace it. |
| Other local `.env` secrets | AAC's `setup-local-secrets.sh` | Disposable local values only; do not use for AAT or production. |
| Deployed secrets | Service secret store and pipeline configuration | Keep using the existing deployed mappings. |


### IDAM Configuration

- Create ACA test roles, services and users using scripts located in the bin directory.

    Export following variables required for the scripts to run
    ```bash
    export IDAM_ADMIN_USER=<enter email>
    export IDAM_ADMIN_PASSWORD=<enter password>
    ```

    The value for `IDAM_ADMIN_USER` and `IDAM_ADMIN_PASSWORD` details can be found on [confluence](https://tools.hmcts.net/confluence/x/eQP3P)

    - To add idam client services (eg: `xuiwebapp`) :

        ```bash
        ./bin/add-idam-clients.sh
        ```

    - To add roles required to import ccd definition:

        ```bash
        ./bin/add-roles.sh
        ```

    - To add users:

        ```bash
        ./bin/add-users.sh
        ```

    - To populate wiremock PRD data with user GUIDS

        ```bash
        ./bin/findPrdUserIds.sh
        ```

        This script updates placeholders in: 

        ```
        aca-docker/mocks/wiremock/__files/prd_users_organisation_01.json
        aca-docker/mocks/wiremock/__files/prd_users_organisation_02.json
        ```

        You need to run this script every time you run `./bin/add-users.sh`, as adding users will
        generate new GUIDs.

        The `aca-wiremock` container will need to be restarted to read in these new GUIDs

        ```bash
        cd aca-docker
        docker-compose -f compose/aca.yml restart aca-wiremock
        ```

### Run `ccd-docker` containers
- Install and run CCD stack as advised [here](https://github.com/hmcts/ccd-docker).

    Please enable following ccd components.
    ```bash
    ./ccd enable backend sidam sidam-local sidam-local-ccd
    ```

    *Memory and CPU allocations may need to be increased for successful execution of ccd applications altogether*

## ACA

Please run aca docker as follows.

```
cd aca-docker
docker-compose -f compose/aca.yml up -d
```

### Compose branches

By default, tha ACA container will be running the `latest` tag, built from the `master` branch.  However, this behaviour can be changed by using the environment variable: `MANAGE_CASE_ASSIGNMENT_TAG`.

#### Switch to a branch

To switch to a branch (e.g. `pr-126`): `set` the environment variable and update the containers:

```bash
export MANAGE_CASE_ASSIGNMENT_TAG=<branch>
docker-compose -f compose/aca.yml up -d
```

#### Revert to `master`

To revert to `master`: `unset` the environment variable and update the containers:

```bash
unset MANAGE_CASE_ASSIGNMENT_TAG
docker-compose -f compose/aca.yml up -d
```

## LICENSE

This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.
