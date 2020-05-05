# Manage Case Assignment

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

### Code quality checks
We use [checkstyle](http://checkstyle.sourceforge.net/) and [PMD](https://pmd.github.io/).  
To run all checks execute the following command:
```bash
./gradlew clean checkstyleMain checkstyleTest checkstyleIntegrationTest pmdMain pmdTest pmdIntegrationTest
```

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.
