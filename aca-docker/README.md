# ACA fullstack docker :whale:

- [Prerequisites](#prerequisites)
- [ACA Scripts](#ACA)
- [License](#license)

## Prerequisites

- [Docker](https://www.docker.com)
- Install and run CCD stack as advised [here](https://github.com/hmcts/ccd-docker).
  You would need to register aca and xuiwebapp services under `service-auth-provider` docker config in the ccd.
  ```
  MICROSERVICEKEYS_AAC_MANAGE_CASE_ASSIGNMENT: "${S2S_KEY_MANAGE_CASE_ASSIGNMENT}"
  MICROSERVICEKEYS_XUI_WEBAPP: "${S2S_KEY_XUI_WEBAPP}"
  ```
  Please enable and start elasticsearch along with other ccd components.
  ```
  ./ccd enable backend sidam sidam-local sidam-local-ccd elasticsearch
  ```
*Memory and CPU allocations may need to be increased for successful execution of ccd applications altogether*

## ACA

Please run aca docker as follows. 
```
> cd aca_docker
> docker-compose -f compose/aca.yml up -d
```

Scripts to create ACA test roles, services and users are located in the bin directory.

Export following variables.
```
export IDAM_ADMIN_USER=<enter email>
export IDAM_ADMIN_PASSWORD=<enter password>
```
`IDAM_ADMIN_USER` and `IDAM_ADMIN_PASSWORD` details can be found on [confluence](https://tools.hmcts.net/confluence/x/eQP3P)

To add idam client services (eg: xui-web) :

```
  ./bin/add-idam-clients.sh
```

To add roles required to import ccd definition:

```
  ./bin/add-roles.sh
```

To add users:

```
./bin/add-users.sh
```

## LICENSE

This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.
