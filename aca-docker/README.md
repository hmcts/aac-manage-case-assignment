# ACA fullstack docker :whale:

- [Prerequisites](#prerequisites)
- [ACA Scripts](#ACA)
- [License](#license)

## Prerequisites

- [Docker](https://www.docker.com)

### Environment Variables
- Ensure the relevant environment variables in `aca-docker\bin\env_variables-all` are set by either

    - exporting them all in your bash profile (`~/.bash_profile`)
    - from `aca-docker/bin` running `source set-environment-variables.sh`
    
    **NOTE**: Using `source set-environment-variables.sh` does not deal with exported values containing spaces.
    If using this method you will need to manually export the following environment variable
    
    ```bash
    export BEFTA_OAUTH2_SCOPE_VARIABLES_OF_XUIWEBAPP="profile openid roles"   
    ```

### IDAM Configuration

- Create ACA test roles, services and users using scripts located in the bin directory.
    
    Export following variables required for the scripts to run
    ```
    export IDAM_ADMIN_USER=<enter email>
    export IDAM_ADMIN_PASSWORD=<enter password>
    ```
    `IDAM_ADMIN_USER` and `IDAM_ADMIN_PASSWORD` details can be found on [confluence](https://tools.hmcts.net/confluence/x/eQP3P)

    - To add idam client services (eg: xui-web) :

    ```
      ./bin/add-idam-clients.sh
    ```
    
    - To add roles required to import ccd definition:
    
    ```
      ./bin/add-roles.sh
    ```
    
    - To add users:
    
    ```
      ./bin/add-users.sh
    ```

    - To populate wiremock PRD data with user GUIDS
    
    ```bash
      ./bin/findPrdUserIds.sh
    ```
  
    This script updates placeholders in `aca-docker/mocks/wiremock/__files/prd_users.json`
    
    **NOTE** You will need `psql` (at least version 12.4) installed to run this script
    
    You need to run this script every time you run `./bin/add-users.sh`, as adding users will
    use generate new GUIDs.
    
    The `aca-wiremock` container will need to be restarted to read in these new GUIDs
    
    ```
    > cd aca_docker
    > docker-compose -f compose/aca.yml restart -aca-wiremock
    ```

### Run `ccd-docker` containers  
- Install and run CCD stack as advised [here](https://github.com/hmcts/ccd-docker).
  
    Please enable elasticsearch along with other ccd components.
      
    ```
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
> cd aca_docker
> docker-compose -f compose/aca.yml up -d
```

## LICENSE

This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.
