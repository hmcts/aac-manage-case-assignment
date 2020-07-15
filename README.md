# Manage Case Assignment
add something
This micro-service provides a set of APIs to manage case access. 

**TODO**: add more description / architecture diagram etc 

## Getting Started

### Prerequisites
- [JDK 11](https://java.com)

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
This image is available in the HMCTS azure container registry. Image url is: `hmctspublic.azurecr.io/aac/manage-case-assignment`  
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

### Functional tests
These are the tests run against an environment. For example if you would like to test your local
 environment you'll need to export the following variables on your `.bash_profile` script.

```bash
#Functional Tests
export TEST_URL=http://localhost:4454
export S2S_URL=http://localhost:4502
export IDAM_URL=http://localhost:5000
export BEFTA_S2S_CLIENT_ID=xui_webapp
export BEFTA_S2S_CLIENT_SECRET=AAAAAAAAAAAAAAAC
export CCD_CASEWORKER_AUTOTEST_EMAIL=someemail@blob.com
export CCD_CASEWORKER_AUTOTEST_PASSWORD=XYZT
export CCD_IMPORT_AUTOTEST_EMAIL=someemail@blob.com
export CCD_IMPORT_AUTOTEST_PASSWORD=XYZT
```

> Note: For details of the emails and passwords to use in a local environment see the 
 [Create users and roles](https://github.com/hmcts/ccd-docker#3-create-users-and-roles) steps in
 [ccd-docker](https://github.com/hmcts/ccd-docker) project.

These tests can be run using:
```bash
./gradlew functional
```

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
