# GITB Engine
## Build
The GITB Test Bed implementation uses Maven as a build automation and dependency management tool. This project can be built by executing the following command within the root folder of the project:

```sh
$ mvn clean install -DskipTests=true
```

This command builds and compiles all necessary files and creates a war file located in the *gitb-testbed-service/target/gitb-testbed-service-1.0-SNAPSHOT.war*

## Development environment

When making a development build, the URL for the test case repository needs to be set. This is done by providing
environment variables as follows:
- remote.testcase.repository.url: http://localhost:9000/repository/tests/:test_id/definition
- remote.testresource.repository.url: http://localhost:9000/repository/resource/:test_id/:resource_id

By default these are set to work with docker containers as defined in /gitb-remote-testcase-repository/src/main/resources/remote-testcase-repository.properties

## Running

The GITB Test Bed can be run after a successful build by executing the following command within the project's gitb-testbed-service module's folder:

```sh
$ mvn spring-boot:run
```

# GITB UI
The GITB Test Bed has a test control & management application located under the `gitb-ui` folder. This project is an SBT project that uses the [Play! Framework](http://www.playframework.com/).

## Dependencies
The GITB UI application assumes that the following tools are installed and running:

* Redis
* MySQL RDBMS
* GITB Engine

## Configuration
The GITB-UI project can be configured using the parameters located in `gitb-ui/conf/application.conf` file.

### HTTP Server
It listens on port **9000** by default. This can be configured by either setting the `http.port` configuration in `gitb-ui/conf/application.conf` file or running the application with the `-Dhttp.port=${GITB_UI_PORT}` parameter.

### MySQL Database
For this application to work properly, the following DB configurations must be set in `gitb-ui/conf/application.conf`:

* db.default.url="jdbc:mysql://localhost/gitb?characterEncoding=UTF-8&useUnicode=true&autoReconnect=true"
* db.default.user=...
* db.default.password=...
* db.default.rooturl="jdbc:mysql://localhost/"
* db.default.name="gitb"

Note that if running with a VM-based Docker DB "localhost" would need to be replaced with typically "192.168.99.100".

### Redis Server
A Redis Server is assumed to be available at the `127.0.0.1:6379` address. This can be configured by setting the parameters in `gitb-ui/conf/application.conf`:

* redis.host = "127.0.0.1"
* redis.port = 6379

Similarly, if Redis is running via a VM-based Docker machine replace "127.0.0.1" with (typically) "192.168.99.100".

### GITB Engine
To run the tests, an instance of the GITB Engine should be up and running. The **Testbed Service** endpoint for the GITB Engine instance can be configured by setting the `testbed.service.url` parameter in `gitb-ui/conf/application.conf`.

## Build & Running
This application is using the [Play! Framework](http://www.playframework.com/) that supports building an all-in-one executable via SBT. The following is a list fo the most common SBT commands ( to be ran from the `gitb-ui` directory):

* `./sbt clean` to clean the application
* `./sbt update` to pull all dependencies
* `./sbt compile` to build the application
* `./sbt run` to run the application in **debug** mode
* `./sbt clean dist` to create a standalone distribution. This command creates a zip file located under `gitb-ui/target/universal/` folder. You can unzip this file to the deployment folder. After this, there should be an executable script located in `bin/gitb-ui` path. This script can be executed directly to start the application in **production** mode.

These build commands are equal for both docker and local deployment. The application config file defaults to a local deployment and is working with environment variables for the docker container.
This way no additional steps have to be taken depending for which deployment we are building for.

A fresh test bed installation is configured to work with functional accounts where the test bed admin is:
* Username: test@test.com
* Password: test
