# Manage Case Assignment


[![API Docs](https://img.shields.io/badge/API%20Docs-site-e140ad.svg)](https://hmcts.github.io/cnp-api-docs/swagger.html?url=https://hmcts.github.io/cnp-api-docs/specs/aac-manage-case-assignment.json)

This micro-service provides a set of APIs to manage case access.

**TODO**: add more description / architecture diagram etc

## Getting Started

### Prerequisites
- [JDK 21](https://java.com)

### Building
The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:
```bash
./gradlew build
```

### Running
Run the application by executing:
```bash
./gradlew bootRun
```

### Consuming this service locally
This image is available in the HMCTS azure container registry. Image url is: `hmctsprod.azurecr.io/aac/manage-case-assignment`
See required config in: [docker-compose.yml](docker-compose.yml)


## API documentation
API documentation is provided with Swagger.
You can view the json spec here: [http://localhost:4454/v2/api-docs](http://localhost:4454/v2/api-docs)
Swagger UI is available here: [http://localhost:4454/swagger-ui.html](http://localhost:4454/swagger-ui.html)

## Developing

### Unit tests
To run all unit tests execute the following command:
```bash
./gradlew test
```

### Integration tests
To run all integration tests execute the following command:
```bash
./gradlew integration
```

### Provider Pact tests

In order to run the provider Pact tests locally, first start the Postgres database and Pact Broker services from the [Pact Broker Docker repository](https://github.com/pact-foundation/pact-broker-docker):

```bash
git clone https://github.com/pact-foundation/pact-broker-docker.git
cd pact-broker-docker
docker compose up -d postgres pact-broker
```

Second, from the consumer repository, ensure that you have published the consumer pact under test to the local broker with the `Dev` tag. 


Finally, from this repository, run the provider verification and publish the verification results back to the local Pact Broker:

```bash
PACT_BROKER_SCHEME=http \
PACT_BROKER_URL=localhost \
PACT_BROKER_PORT=9292 \
PACT_CONSUMER_TAG=Dev \
./gradlew runProviderPactVerification \
  -Ppact.verifier.publishResults=true \
  --rerun-tasks
```


When finished, stop the broker with `docker compose down` from the `pact-broker-docker` directory.

### Functional tests
These are the tests run against an environment. For example if you would like to test your local
 environment you'll need to export variables on your `.bash_profile` script.

Details of the relevant environment variables can be found in the `aca-docker/README.md`

> Note: For details of the emails and passwords to use in a local environment see the
 [Create users and roles](https://github.com/hmcts/ccd-docker#3-create-users-and-roles) steps in
 [ccd-docker](https://github.com/hmcts/ccd-docker) project.

#### To Run the Functional Tests (FT)

#####  All Functional Tests
Will run all the FT's:

```bash
./gradlew functional
```

#####  Some Functional Tests
Will run both F-1023 and F-777:

```bash
./gradlew functional -P tags="@F-1023 or @F-777"
```


Will run only S-1023.5:

```bash
./gradlew functional -P tags="@S-1023.5"
```

These tests can be run using:


### Code quality checks
We use [checkstyle](http://checkstyle.sourceforge.net/) and [PMD](https://pmd.github.io/).
To run all checks execute the following command:

```bash
./gradlew clean checkstyleMain checkstyleTest checkstyleIntegrationTest pmdMain pmdTest pmdIntegrationTest
```

### Docker
Create docker image:

```bash
  docker-compose build
```

Run the distribution by executing the following command:

```bash
  docker-compose up
```
This will start the API container exposing the application's port 4454.

By default, docker-compose.yml is pointing to AAT urls of all downstream dependencies. So, you need to enable hmcts proxy.

You can spin-up full aca docker stack locally. Instructions are available under `aca-docker/README.md`.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.
