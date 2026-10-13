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

### Consumer contract tests

Run the Pact consumer tests against a local mock provider:

```bash
./gradlew contractTest
```

These tests also run as part of `check` and `build`. They exercise `PrdApiClient` with
the production authentication interceptor and JSON mapper, covering:

| Consumer | Provider | Request |
| --- | --- | --- |
| `aac_manageCaseAssignment` | `referenceData_organisationalExternalUsers` | `GET /refdata/external/v1/organisations/users?status=Active&returnRoles=false` |

The successful response requires an organisation identifier and a list of users
with the identifiers, names and email addresses used by case assignment. Pact
type matchers allow different user details and additional response fields; the
list may contain more than one user.

The generated contract is
`pacts/aac_manageCaseAssignment-referenceData_organisationalExternalUsers.json`.
Publish it to the configured broker after running the tests:

```bash
PACT_BROKER_URL=https://pact-broker.platform.hmcts.net PACT_BRANCH_NAME=<branch> ./gradlew pactPublish
```

For a local broker running on port 9292, run the tests and publish with a local tag:

```bash
PACT_BROKER_URL=http://localhost:9292 PACT_BRANCH_NAME=local ./gradlew contractTest pactPublish
```

View the published contract in the [local Pact Broker](http://localhost:9292).
The broker stores contracts and verification results; consumer tests use the Pact
mock server, and provider verification runs separately against Ref Data.

Provider verification uses the state `Professional users exist for an Active organisation`
under `referenceData_organisationalExternalUsers`. The local Ref Data checkout now
includes `CaseAssignmentExternalUsersProviderTest`, which reuses its existing
professional external-users controller test configuration for this participant.
It verifies the real controller with fixture service responses and publishes the
result when `-Ppact.verifier.publishResults=true` is supplied. A published provider
result populates the broker's **Last verified** column.

The internal organisation endpoints listed in the gap analysis use
`referenceData_organisationalInternal` and need separate contracts. The existing
`contract` task runs this service's provider verification tests.

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
