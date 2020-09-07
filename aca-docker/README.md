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
- Ensure the relevant environment variables in `aca-docker/bin/env_variables-all.txt` are set by running 
    
    ```bash
    cd aca-docker/bin
    source env_variables_all.txt
  ```
    

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
  
    This script updates placeholders in `aca-docker/mocks/wiremock/__files/prd_users.json`
        
    You need to run this script every time you run `./bin/add-users.sh`, as adding users will
    use generate new GUIDs.
    
    The `aca-wiremock` container will need to be restarted to read in these new GUIDs
    
    ```bash
    > cd aca-docker
    > docker-compose -f compose/aca.yml restart -aca-wiremock
    ```

### Run `ccd-docker` containers  
- Install and run CCD stack as advised [here](https://github.com/hmcts/ccd-docker).
  
    Please enable elasticsearch along with other ccd components.
      
    ```bash
    ./ccd enable backend sidam sidam-local sidam-local-ccd elasticsearch
    ```
  
    before starting the containers, ensure the `ES_DOCKER_ENABLED` environment variable 
    (listed in `aca-docker\bin\env_variables-all` ) is set to `true`
  
    If `ccd-docker` was running before `ES_DOCKER_ENABLED` was set, you will need to restart the 
    `definiton-store-api` and `data-store-api` containers to pick up this new environment variable
    
    *Memory and CPU allocations may need to be increased for successful execution of ccd applications altogether*

## ACA

Please run aca docker as follows. 
```
> cd aca-docker
> docker-compose -f compose/aca.yml up -d
```

## LICENSE

This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.
