# GITB Engine
## Build
GITB PoC Testbed implementation uses Maven as a build automation and dependency management tool. This project can be built by executing the following command within the root folder of the project:

```sh
$ mvn clean install -DskipTests=true -Denv=<environment>
```

There are two different environments for which we can build `dev` or `docker`. `Dev` being the default and thus specifying is optional.

This command builds and compiles all necessary files and creates a war file located in the *gitb-testbed-service/target/gitb-testbed-service-1.0-SNAPSHOT.war*


## Running

GITB PoC Testbed implementation can be run after a successful build by executing the following command within the root folder of the project:

```sh
$ mvn jetty:run -pl ./gitb-testbed-service/
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

### Redis Server
By default, it assumed that there is a Redis Server available at the `127.0.0.1:6379` address. This can be configured by setting the parameters in `gitb-ui/conf/application.conf`:

* redis.host = "127.0.0.1"
* redis.port = 6379

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

## Test Suite Deployment
Currently only users with *System Admin* roles can deploy test suites. Test suites are deployed using zip files.

One of the critical things about the structure of the test suite archives is the test suite definition xml file. This file is formatted according to the `TestSuite` type defined in the `gitb-tdl` schema (ns: http://www.gitb.com/tdl/v1/) and it should be located at the root of the test suite archive.

After configuring a user as *System Admin*, when this user is logged in to the system `Admin` link appears at the top navigation bar.

To deploy a test suite, a domain and a specification related to the test suite must be created. Then, in the specification detail page, by using the `Deploy test suite` button, a test suite archived can be uploaded. If the deployment is successful, test cases and actors related to the test suite should be added to the system.

### Sample Test Suites

The application currently contains two sample test suites:

* `test-suites/Peppol_BIS_3A_Order`
* `test-suites/Peppol_BIS_4A_Invoice`
* `test-suites/Peppol_BIS_30A_DespatchAdvice`
* `test-suites/ServiceMetadataLocator`
* `test-suites/ServiceMetadataPublisher`

# Version
1.0-SNAPSHOT
