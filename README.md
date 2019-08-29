# GITB Engine
## Build
GITB PoC Testbed implementation uses Maven as a build automation and dependency management tool. This project can be built by executing the following command within the root folder of the project:

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

GITB PoC Testbed implementation can be run after a successful build by executing the following command within the project's gitb-testbed-service module's folder:

```sh
$ mvn spring-boot:run
```

# GITB UI
GITB PoC Testbed has a test control & management application located under `gitb-ui` folder. This project is an SBT project that uses [Play! Framework](http://www.playframework.com/).

## Dependencies
GITB UI application assumes that the following tools are installed and running:

* Redis
* MySQL RDBMS
* GITB Engine

## Configuration
The GITB-UI project can be configured using the parameters located in `gitb-ui/conf/application.conf` file.

### HTTP Server
It listens **9000** port by default. However this can be configured by either setting `http.port` configuration in `gitb-ui/conf/application.conf` file or running the application with `-Dhttp.port=${GITB_UI_PORT}` parameter.

### MySQL Database
For this application to work properly, the following DB configurations must be set in `gitb-ui/conf/application.conf`:

* db.default.url="jdbc:mysql://localhost/gitb?characterEncoding=UTF-8&useUnicode=true&autoReconnect=true"
* db.default.user=...
* db.default.password=...
* db.default.rooturl="jdbc:mysql://localhost/"
* db.default.name="gitb"

Note that if running with a VM-based Docker DB "localhost" would need to be replaced with typically "192.168.99.100".

### Redis Server
By default, it assumed that there is a Redis Server available at the `127.0.0.1:6379` address. This can be configured by setting the parameters in `gitb-ui/conf/application.conf`:

* redis.host = "127.0.0.1"
* redis.port = 6379

Similarly, if Redis is running via a VM-based Docker machine replace "127.0.0.1" with (typically) "192.168.99.100".

### GITB Engine
To run the tests, an instance of the GITB Engine should be up and running. The **Testbed Service** endpoint for the GITB Engine instance can be configured by setting the `testbed.service.url` parameter in `gitb-ui/conf/application.conf`.

### User management

#### User roles
Currently, the following roles are implemented in the project:

* **Vendor Admin**: Vendor system administrator
* **Vendor User**: Vendor system user (tester)
* **System Admin**: Manages the test engine, users, and repositories, creates new domains, specifications, deploys/removes test suites

System admins are created under organization with ID 0. So, a default organization with ID 0 should be available in the **gitb.organizations** table. This organization is otherwise fully hidden in the UI.

## Build & Running
Since this application is using the [Play! Framework](http://www.playframework.com/), an executable that takes care of the build & running process exists in the application folder. To do these operations please execute the following commands in the `gitb-ui` directory:

* `./activator clean compile` to build the application
* `./activator run` to run the application in **debug** mode
* `./activator clean dist` to create a standalone distribution. This command creates a zip file located under `gitb-ui/target/universal/` folder. You can unzip this file to the deployment folder. After this, there should be an executable script located in `bin/gitb-ui` path. This script can be executed directly to start the application in **production** mode.

These build commands are equal for both docker and local deployment. The application config file defaults to a local deployment and is working with environment variables for the docker container.
This way no additional steps have to be taken depending for which deployment we are building for.

A fresh test bed installation is configured to work with functional accounts where the test bed admin is:
* Username: test@test.com
* Password: test
