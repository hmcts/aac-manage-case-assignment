# ACA fullstack docker :whale:

- [Prerequisites](#prerequisites)
- [ACA Scripts](#ACA)
- [License](#license)

## Prerequisites

- [Docker](https://www.docker.com)
- psql (full SIDAM/IDAM only; tested with version 12.4)
- `ccd-docker` environment configured and running,
see [Run `ccd-docker` containers](#Run-ccd-docker-containers) for details

### Environment Variables
- Ensure the relevant environment variables in `aca-docker/bin/env_variables-all.txt` are set by running

    ```bash
    cd aca-docker/bin
    source env_variables_all.txt
  ```


### IDAM Configuration

- Create ACA test users using scripts located in the bin directory.

    The local setup follows `ccd-docker` and uses `idam-sim` by default. With `idam-sim`, run:

    ```bash
    ./bin/add-users.sh
    ```

    Do not run `add-idam-clients.sh` or `add-roles.sh` when using `idam-sim`. Those scripts call
    full IDAM admin endpoints (`/services` and `/roles`) that are not available in the simulator.

    Full SIDAM/IDAM only: export the admin credentials required for the client and role scripts:

    ```bash
    export IDAM_ADMIN_USER=<enter email>
    export IDAM_ADMIN_PASSWORD=<enter password>
    ```

    The value for `IDAM_ADMIN_USER` and `IDAM_ADMIN_PASSWORD` details can be found on [confluence](https://tools.hmcts.net/confluence/x/eQP3P)

    - Full SIDAM/IDAM only: to add idam client services (eg: `xuiwebapp`) :

        ```bash
        ./bin/add-idam-clients.sh
        ```

    - Full SIDAM/IDAM only: to add IDAM roles:

        ```bash
        ./bin/add-roles.sh
        ```

    - To populate wiremock PRD data with user GUIDS

        ```bash
        ./bin/findPrdUserIds.sh
        ```

        With the default `idam-sim` setup this script resolves user IDs from
        `IDAM_API_BASE_URL` using the simulator test-support account endpoint.
        If that endpoint is not available, it falls back to the existing full
        SIDAM/IDAM Postgres lookup on `localhost:5432`.

        This script updates placeholders in:

        ```
        aca-docker/mocks/wiremock/__files/prd_users_organisation_01.json
        aca-docker/mocks/wiremock/__files/prd_users_organisation_02.json
        ```

        You need to run this script every time you run `./bin/add-users.sh`, as adding users will
        generate new GUIDs.

        If the `aca-wiremock` container is running, the script restarts it
        automatically so WireMock reads the generated GUIDs. If Docker is not
        available or the container is not running, restart `aca-wiremock`
        manually before running functional tests.

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
