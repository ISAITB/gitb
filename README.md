# Introduction

This repository contains the main application components that are used by the [Interoperability Test Bed](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed).

## What is the Interoperability Test Bed?

The Interoperability Test Bed is a service offered by the European Commission’s DIGIT for the **conformance testing** of IT systems.
It approaches conformance testing by means of scenario-based test cases expressed in the [GITB Test Description Language (TDL)](https://www.itb.ec.europa.eu/docs/tdl/latest/),
that defines test steps implemented using built-in capabilities or by orchestration of [extension services](https://www.itb.ec.europa.eu/docs/services/latest/).

Test cases typically involve **message exchanges** with a system under test, with **validation** of individual messages and the overall conversation,
but also steps including control flow, user interactions, and arbitrary processing of test session data. Besides the test engine itself, the Test Bed also
provides a **rich user interface** for administrators and testers, as well as a **REST API** and **webhooks** for integrations and automation workflows.

DIGIT operates its own [Test Bed instance](https://www.itb.ec.europa.eu/itb/) shared by several projects, but also makes
available all software as components (shared as public Docker images) that can be installed and used locally.   

## About this repository

This repository contains the main application components that are used by the Interoperability Test Bed. These are:
- The **test engine**, responsible for executing test cases and reporting their progress and outcome.
  In terms of Docker images, this component represents the ``gitb-srv`` image.
- The **frontend**, responsible for the Test Bed's user interface and its test, user, and configuration
  management features. In terms of Docker images, this component represents the ``gitb-ui`` image.

In subsequent sections these components are referred to using their Docker image names (``gitb-srv`` and ``gitb-ui``).

> **Note**
> 
> This repository is used to build the Test Bed's software components from their source. The simplest approach
> to use the Test Bed is via its **published Docker images** ([gitb-ui](https://hub.docker.com/r/isaitb/gitb-ui) and [gitb-srv](https://hub.docker.com/r/isaitb/gitb-srv)) following
> our [installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBed/index.html).
> 
> Alternatively, you may build and launch the Docker service directly from the provided sources. See [here](#build-for-deployment) for details.

## Documentation

The main documentation hub for the Test Bed is its [Joinup space](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed)
from where you can access all [types of documentation](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed/documentation).
Documentation highlights include:
- The [developer onboarding guide](https://www.itb.ec.europa.eu/docs/guides/latest/onboardingDevelopers/index.html), that provides a technical overview of how the Test Bed works.
- The Test Bed's [user guide](https://www.itb.ec.europa.eu/docs/itb-ta/latest/). 
- The reference documentation for the [GITB TDL](https://www.itb.ec.europa.eu/docs/tdl/latest/) (to author test cases) and [GITB test services](https://www.itb.ec.europa.eu/docs/services/latest/) (to add custom extensions). 
- The installation guide [for development](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBed/index.html) and [for production](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html). All other documentation (guides, tutorials, reference documentation) is available [here](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed/documentation-and-resources).

If you are new to the Test Bed you should start with the [developer onboarding guide](https://www.itb.ec.europa.eu/docs/guides/latest/onboardingDevelopers/index.html), and then follow the tutorials
to [create a first test case](https://www.itb.ec.europa.eu/docs/guides/latest/creatingATestCase/index.html), extend it for [simple messaging needs](https://www.itb.ec.europa.eu/docs/guides/latest/sendingAndReceivingContent/index.html),
and then for more [complex testing needs](https://www.itb.ec.europa.eu/docs/guides/latest/developingComplexTests/index.html).

# Technology stack

Both ``gitb-srv`` and ``gitb-ui`` are Java-based applications. Specifically, ``gitb-srv`` is packaged
as a Spring Boot application using Akka as its primary internal framework, whereas ``gitb-ui``
is developed in Scala and uses the Play Framework.

The frontend of the ``gitb-ui`` component is an Angular app developed in TypeScript, and is managed in terms
of scaffolding and build using Angular CLI.

# Configuration properties

The main configuration file of interest is ``application.conf`` from ``gitb-ui`` although this never needs
to be adapted directly. All properties can be set through environment variables, provided either via Docker
or through the IDE used for development.

The full listing and documentation of properties is available [here](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBedProduction/index.html#configuration-properties).

# Build for development

Follow these steps to build and run the application components for development purposes.

## Prerequisites

To build and run the Test Bed's components you need to have the following tools:
- JDK 17+, used as the base platform for both ``gitb-srv`` and ``gitb-ui``.
- Maven 3.9+, used to build ``gitb-srv``.
- SBT 1.9+, used to build ``gitb-ui``.
- Scala 2.13+, used to build the backend app of ``gitb-ui``.
- Node version 20+, used to build the frontend app of ``gitb-ui``.

Although not mandatory, the proposed IDE to use is IntelliJ, and VS Code for ``gitb-ui``'s Angular app.

### Additional Test Bed components

The focus of this README file is the ``gitb-srv`` and ``gitb-ui`` components. To run a complete Test Bed instance
you will also require at least:
- A MySQL database (version 8.*) for its persistence.
- A REDIS instance for the caching of user sessions.

Both these instances are set up separately (e.g. via Docker) environment. These can be set up from Docker
as follows:
- MySQL: ``docker run --name gitb-mysql -p 3306:3306 -d isaitb/gitb-mysql``
- REDIS: ``docker run --name gitb-redis -p 6379:6379 -d redis:7.0.11``

> **Note**  
> All images and containers are defined in ``docker-compose.yml`` and explained in detail the [developer installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBed/index.html). You may build and launch the complete service as described [here](#build-for-deployment).

## Test engine (gitb-srv)

To build and run the ``gitb-srv`` component carry out the following steps:
1. From folder ``gitb`` run ``mvn clean install -DskipTests=true``.
2. Create a run configuration in your IDE as follows (or set up outside your IDE). The run configuration
   can be defined in one of two ways:
   - **As a Java Application (best):** Set class ``Application`` from the ``gitb-testbed-service`` module as
     your entry point. This is the best approach as it allows direct debugging without extra configuration.
   - **Via Maven:** Run ``mvn spring-boot:run`` from the ``gitb-testbed-service`` module.
3. Adapt your run definition's environment variables to add the following:
   - ``remote.testcase.repository.url`` = http://localhost:9000/repository/tests/:test_id/definition
   - ``remote.testresource.repository.url`` = http://localhost:9000/repository/resource/:test_id/:resource_id

Note that defining a run configuration is something specific. If running outside an IDE you can follow either
approach but ensure that the environment variables are correctly set up.

## Frontend (gitb-ui)

To build and run the ``gitb-ui`` component carry out the following steps:
1. From the ``gitb-ui`` folder issue ``sbt compile``.
2. Define in your IDE a run configuration as a sbt application to run ``sbt run``.
3. Set environment variables as needed. For example set ``THEME = ec`` for a EC-themed UI.

The next step is to choose how to build and run the Angular UI application. Its resources are defined in
the ``gitb-ui/ui`` folder and its build is managed by Angular CLI. You have two options on how to work with
the Angular app - choose the preferred approach for you from the two following sections. As a common first
step regardless of the approach you choose you need to issue ``npm install`` from folder ``gitb-ui/ui``. 

**Developing with EU Login enabled:** In case you are developing with EU Login enabled (i.e. against a local EU 
Login test instance), you need to ensure that the following environment variables are set:
- ``SESSION_SECURE`` set to ``false`` 
- ``SESSION_COOKIE_NAME`` set to ``ITB_SESSION``.

Failure to set these properties will result in session cookies never getting served over HTTP, resulting in endless
authentication loops. The alternative is to test over HTTPS via a local SSL-enabled proxy (e.g. a local nginx server
with a self-signed server certificate).

### Option 1: Work on Angular UI through Play application

Using this approach you build the Angular app using Angular CLI but access it through the Play application. To do this:
1. From ``gitb-ui/ui`` issue ``npm run build``. This will build the app and copy it under the Play application's ``assets``
   folder. To rebuild automatically for any changes use ``npm run build:dev``.
2. Access the application by first going to the Play application's welcome page at http://localhost:9000. Once you click
   any of the login options the Angular app will be launched.
3. Once any part of the Angular app is rebuilt you will need to refresh the browser page to see
   changes.     

**When to use this approach:** Using this approach mirrors exactly how the application will run in production. In addition,
you may need to use this approach to effectively test aspects that require interaction with the Play application. These 
include:
- Testing with EU Login enabled (as EU Login's callback returns to the Play application).
- Testing the different login options (e.g. register, demos, confirm role assignment).

### Option 2: Work on Angular UI standalone 

Using this approach you build and serve the Angular app using Angular CLI. In this scenario the Play application
is only used through its REST API which the Angular app proxies and uses. To use this approach:
- Only once from ``gitb-ui/ui`` issue  ``npm run build``. This is needed to put in place static resources used by the Angular app
  (most notably tinymce styles).
- From ``gitb-ui/ui`` issue ``npm start``. This reloads the app upon detected changes.
- Access the application at http://localhost:4200 (this will be automatically opened for you).  

**When to use this approach:** This approach is the most efficient as it allows use of the lightweight CLI server
and provides automatic refresh for build changes. The problem with this approach is that it cannot cover
cases where you need to test interactions with the Play application (e.g. when EU Login is used for authentication).
Having said this however, you can still use this after you have authenticated using the Play application given that
authentication cookies are shared.

# Build for deployment

Follow these steps to build the components for deployment as a Dockerised service. Two approaches are available:
* [Build using the project's development tools](#build-using-development-tools).
* [Build using Docker Compose (containerised build)](#build-using-docker-compose).

## Build using development tools

This approach is more suitable is you have set up the repository for development and have [installed all necessary tooling](#prerequisites).
Producing build artefacts is faster but expects the necessary tools (Java, Maven, NPM, SBT, Scala) to be installed, and manual steps
to build Docker images.

### Test engine (gitb-srv)

1. From the current (root) folder issue ``mvn clean install -DskipTests=true -Denv=docker``
2. Copy file ``gitb-testbed-service/target/itbsrv.war`` to a separate folder to build its Docker image.
3. Use Dockerfile ``etc/docker/gitb-srv/Dockerfile`` from the same folder to build the image with `docker build -t isaitb/gitb-srv .`.

### Frontend (gitb-ui)

1. From the ``gitb-ui`` folder issue ``sbt clean dist``. This builds all Play code and automatically calls the required
   prod build target from its Angular app.
2. Unzip the contents of the ``gitb-ui/target/universal/gitb-1.0-SNAPSHOT.zip`` archive to a separate folder. The name of the resulting unzipped folder should be ``gitb-ui``.
3. Use the Dockerfile ``etc/docker/gitb-ui/Dockerfile`` from the same folder to build the image with `docker build -t isaitb/gitb-ui .`.

### Database (gitb-mysql)

1. From the `etc/docker/gitb-mysql` folder build the image with `docker build -t isaitb/gitb-mysql .`

### Create the overall service

1. Follow the [development installation guide](https://www.itb.ec.europa.eu/docs/guides/latest/installingTheTestBed/index.html).
2. Create or [download](https://www.itb.ec.europa.eu/docs/guides/latest/_downloads/d863ccca4f45b40bb17be79b4841ad4b/docker-compose.yml) your docker-compose.yml file
   making use of the locally built images (see previous steps).
3. Launch the service from the folder containing your `docker-compose.yml` file with `docker compose up -d`.

## Build using Docker Compose

This approach is more suitable for a containerised build, avoiding any manual steps and developer tool installations (apart from Docker Compose). Make sure that your 
Docker Compose is at least at version `2.0.0`. Note that the overall build time in this case is slower compared to building with the relevant tools.

To build and launch all containers, issue from the repository's root folder: `docker compose up -d --build`.

# Using the application

Once a complete Test Bed instance has been set up, either in development or as a Dockerised service, access using the default
Test Bed administrator account as follows:
1. Go to http://localhost:9000.
2. Click on the login button.
3. Authenticate using ``admin@itb``. This account is set with a one-time password that is refreshed at start-up until a
   first login is made. The password to use is obtained by checking the logs of container `gitb-ui`.

An example log output to retrieve the administrator password is as follows: 
```
###############################################################################

The one-time password for the default administrator account [admin@itb] is:

b1afbc39-8ad7-49f4-a9d9-0bcec942aef4

###############################################################################
```

For information on how to proceed once you have logged in, you may refer to the Test Bed's [user guide](https://www.itb.ec.europa.eu/docs/itb-ta/latest/) 
and [sample usage tutorials](https://www.itb.ec.europa.eu/docs/guides/latest/definingYourTestConfiguration/index.html).

# Licence

This software is shared under the European Union Public Licence (EUPL) version 1.1, featuring a CEN-specific extension for 
the GITB CWA to limit its liability and trademark use. See [here](https://github.com/ISAITB/gitb/blob/master/LICENCE.md) for details.

# Legal notice

The authors of this software waive any and all liability linked to its usage, or the interpretation of any results it may produce.
Any data, including data of private nature that it may store, is defined by the eventual downstream user's configuration. A detailed
legal notice of the [shared instance](https://www/itb.ec.europa.eu/itb) hosted by the European Commission is available via the 
relevant welcome page link. 

# Contact

For feedback or questions regarding the GITB Test Bed software you are invited to post issues in the current repository. In addition,
feel free to contact the Test Bed team via email at [DIGIT-ITB@ec.europa.eu](mailto:DIGIT-ITB@ec.europa.eu).

# See also

For general information on all aspects of the Interoperability Test Bed and the GITB software in particular you may refer
to its [Joinup space](https://joinup.ec.europa.eu/collection/interoperability-test-bed-repository/solution/interoperability-test-bed).

The GITB software is used to realise a complete conformance testing platform. If you are more interested in standalone 
data validation you may find interesting the Test Bed's validators for [XML](https://github.com/ISAITB/xml-validator),
[RDF](https://github.com/ISAITB/shacl-validator), [JSON](https://github.com/ISAITB/json-validator) and [CSV](https://github.com/ISAITB/csv-validator)
content.